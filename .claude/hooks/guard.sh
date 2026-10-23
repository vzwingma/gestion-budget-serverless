#!/usr/bin/env bash
# Harness guard — PreToolUse hook (AP 005 P4, ADR 003).
#
# Deterministic enforcement of CLAUDE.md "Règles absolues", complementing permissions.deny
# (which does not catch `sh -c '...'`, `/bin/rm`, `git -C . reset --hard`, SQL in any command):
#   - Bash | PowerShell          : destructive commands (deletion, irreversible git, destructive SQL)
#                                  + commands that mention a .copilotignore path
#   - Edit | Write | NotebookEdit: .copilotignore paths + role boundaries (role-boundaries.conf,
#                                  keyed on the agent_type field of the hook input)
# Blocks with exit code 2 (reason on stderr). Pure bash builtins, no subprocess and no command
# substitution (forks are slow on Git Bash); portable (bash >= 3.2).
# Read/Grep/Glob are covered by static Read() deny rules in settings.json, not by this hook.

IFS= read -r -d '' input || true

# Helpers return their result in REPLY.
json_field() {
    # JSON string field (first occurrence), not unescaped.
    local re="\"$1\"[[:space:]]*:[[:space:]]*\"((\\\\.|[^\"\\\\])*)\""
    REPLY=
    if [[ $input =~ $re ]]; then REPLY=${BASH_REMATCH[1]}; fi
}

norm_path() {
    # JSON-escaped Windows or POSIX path -> forward slashes, single separators, no trailing slash.
    local p=${1//\\//}
    while [[ $p == *//* ]]; do p=${p//\/\///}; done
    REPLY=${p%/}
}

block() {
    printf '⛔ Garde-fou harnais : %s\nRègles absolues : CLAUDE.md. Si l'\''opération est voulue, le 👤 Développeur humain l'\''exécute lui-même.\n' "$1" >&2
    exit 2
}

json_field tool_name;  tool=$REPLY
json_field agent_type; agent=$REPLY
json_field cwd;        norm_path "$REPLY"; cwd=$REPLY
norm_path "${CLAUDE_PROJECT_DIR:-$cwd}";  base=$REPLY
if [[ -z $base ]]; then norm_path "$PWD"; base=$REPLY; fi
hooks_dir="$base/.claude/hooks"

# ── .copilotignore patterns (gitignore approximation, negation unsupported) ──
ignore_patterns=()
if [[ -f "$base/.copilotignore" ]]; then
    while IFS= read -r line || [[ -n $line ]]; do
        line=${line%$'\r'}
        line=${line#"${line%%[![:space:]]*}"}
        line=${line%"${line##*[![:space:]]}"}
        [[ -z $line || $line == \#* || $line == \!* ]] && continue
        ignore_patterns+=("$line")
    done < "$base/.copilotignore"
fi

path_is_ignored() {
    # $1 = path relative to project root. Caller sets nocasematch.
    local rel=$1 pat anchored
    for pat in "${ignore_patterns[@]}"; do
        anchored=0
        if [[ $pat == /* ]]; then anchored=1; pat=${pat#/}; fi
        pat=${pat//\*\*/*}
        if [[ $pat == */ ]]; then
            pat=${pat%/}
            [[ $rel == $pat/* ]] && return 0
            [[ $anchored == 0 && $rel == */$pat/* ]] && return 0
        else
            [[ $rel == $pat || $rel == $pat/* ]] && return 0
            [[ $anchored == 0 && $pat != */* && ( $rel == */$pat || $rel == */$pat/* ) ]] && return 0
        fi
    done
    return 1
}

relative_to_project() {
    # REPLY = path relative to cwd or project root; empty when outside both.
    local p=$1 root
    REPLY=
    shopt -s nocasematch
    for root in "$cwd" "$base"; do
        if [[ -n $root && $p == "$root"/* ]]; then REPLY=${p:${#root}+1}; break; fi
    done
    shopt -u nocasematch
}

# ── Shell commands ────────────────────────────────────────────────────────────
check_command() {
    local cmd=$1 c lit re
    cmd=${cmd//\\\"/\"}
    cmd=${cmd//\\n/ }
    c=" ${cmd//git rm /git_rm } "     # `git rm` is recoverable: left to the permission `ask` rule

    shopt -s nocasematch
    re='[^[:alnum:]_.-](rm|rmdir|unlink|shred|del|erase|remove-item|clear-content)([[:space:]]|$)'
    if [[ $c =~ $re ]]; then shopt -u nocasematch; block "suppression de fichiers (${BASH_REMATCH[1]})"; fi
    re='(drop[[:space:]]+(table|database|schema|view|index)|truncate[[:space:]]+(table[[:space:]]+)?[[:alnum:]_"`]|mkfs|format[[:space:]]+[a-z]:)'
    if [[ $c =~ $re ]]; then shopt -u nocasematch; block "commande destructive (${BASH_REMATCH[1]})"; fi
    re='delete[[:space:]]+from[[:space:]]'
    if [[ $c =~ $re && ! $c =~ [[:space:]]where[[:space:]] ]]; then shopt -u nocasematch; block "DELETE sans clause WHERE"; fi
    shopt -u nocasematch

    # git: case-sensitive (branch -D vs -d)
    re='git[[:space:]]+([^;&|]*[[:space:]])?(clean([[:space:]]|$)|reset[[:space:]]+--(hard|merge|keep)|branch[[:space:]]+(-D|--delete[[:space:]]+--force)|stash[[:space:]]+(drop|clear)|checkout[[:space:]][^;&|]*--[[:space:]]|filter-branch|reflog[[:space:]]+expire|update-ref[[:space:]]+-d)'
    [[ $c =~ $re ]] && block "commande git irréversible (${BASH_REMATCH[2]})"

    re='git[[:space:]]+([^;&|]*[[:space:]])?push[^;&|]*[[:space:]](--force|--mirror|--delete|-f([[:space:]]|$)|\+[^[:space:]]|:[^[:space:]])'
    [[ $c =~ $re ]] && block "git push destructif (${BASH_REMATCH[2]})"

    re='git[[:space:]]+([^;&|]*[[:space:]])?restore[[:space:]]'
    if [[ $c =~ $re && $c != *--staged* ]]; then block "git restore (abandon de modifications non commitées)"; fi

    norm_path "$c"; c="$REPLY "       # backslash paths (PowerShell, JSON-escaped) -> forward slashes
    shopt -s nocasematch
    for lit in "${ignore_patterns[@]}"; do
        [[ $lit == *[\*\?\[]* ]] && continue
        lit=${lit#/}
        [[ -z $lit ]] && continue
        if [[ $c == *[[:space:]\"\'=/]"$lit"* ]]; then
            shopt -u nocasematch; block "accès à un chemin listé dans .copilotignore ($lit)"
        fi
    done
    shopt -u nocasematch
}

# ── File writes ───────────────────────────────────────────────────────────────
check_role() {
    # $1 = agent name, $2 = path relative to project root.
    # Ordered rules: the first line matching the path decides (allow -> ok, deny -> blocked).
    # No match: blocked if the agent has at least one allow line (allow-list), allowed otherwise.
    local who=$1 rel=$2 conf="$hooks_dir/role-boundaries.conf" a mode pat has_allow=0
    [[ -z $who || ! -f $conf ]] && return 0
    shopt -s nocasematch
    while read -r a mode pat || [[ -n $a ]]; do
        pat=${pat%$'\r'}
        [[ -z $a || $a == \#* || -z $pat || $a != "$who" ]] && continue
        [[ $mode == allow ]] && has_allow=1
        if [[ $rel == $pat ]]; then
            shopt -u nocasematch
            [[ $mode == allow ]] && return 0
            block "$who n'édite pas « $rel » (frontière de rôle : $pat)"
        fi
    done < "$conf"
    shopt -u nocasematch
    [[ $has_allow == 1 ]] && block "$who n'édite pas « $rel » (hors périmètre autorisé du rôle)"
    return 0
}

case "$tool" in
    Bash|PowerShell)
        json_field command
        check_command "$REPLY"
        ;;
    Edit|Write|NotebookEdit|MultiEdit)
        json_field file_path
        [[ -z $REPLY ]] && json_field notebook_path
        norm_path "$REPLY"
        relative_to_project "$REPLY"; rel=$REPLY
        if [[ -n $rel ]]; then
            shopt -s nocasematch
            if path_is_ignored "$rel"; then shopt -u nocasematch; block "écriture d'un fichier listé dans .copilotignore ($rel)"; fi
            shopt -u nocasematch
            check_role "$agent" "$rel"
        fi
        ;;
esac
exit 0
