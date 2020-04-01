#!/usr/bin/env sh
TAG=$1

sed -i -e "s|'com.github.DaikonWeb:topinambur:.*'|'com.github.DaikonWeb:topinambur:${TAG}'|g" README.md && rm README.md-e
sed -i -e "s|<version>.*</version>|<version>${TAG}</version>|g" README.md && rm README.md-e

git commit -am "Release ${TAG}"
git tag $TAG
git push
git push --tags
