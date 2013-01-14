#!/bin/bash

VERSION="2.08"

pushd ..

rm -Rf j-interop-code
svn export svn://svn.code.sf.net/p/j-interop/code/trunk j-interop-code
git add j-interop-code

echo "git commit"
echo "git tag 'Vendor $VERSION'"

popd

