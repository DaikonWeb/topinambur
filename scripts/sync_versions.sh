#!/usr/bin/env sh
TAG=$1

if [[ "$TAG" == "" ]]
then
    echo "Enter a valid version to publish!"
    echo ""
    echo "Usage:"
    echo "$0 [TOPINAMBUR_VERSION]"
    exit 1
fi

TOPINAMBUR_ROOT="$(dirname $0)/.."
SCRIPT_NAME="publish.sh"

include_only_topinambur_extensions() {
    cat - | grep "./topinambur-"
}

extensions_scripts_folders() {
    find "${TOPINAMBUR_ROOT}/.." -name "${SCRIPT_NAME}" | include_only_topinambur_extensions | sed "s/${SCRIPT_NAME}$//"
}

for folder in $(extensions_scripts_folders)
do
    (
        cd ${folder}
        echo "deploy: $(pwd)"
        git stash
        git pull --rebase
        git pull --tags
        ./${SCRIPT_NAME} ${TAG}
        git stash pop || true
    )
done