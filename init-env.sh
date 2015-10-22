#!/bin/bash
set -xe

# Create virtual env
pip install virtualenv
rm -rf venv
virtualenv venv -p /usr/bin/python2.7
source venv/bin/activate

# Install dependencies
pip install --download-cache=cache -r muvr.pip

# Insteall neon latest
rm -rf /tmp/neon
git clone https://github.com/NervanaSystems/neon.git /tmp/neon
cd /tmp/neon
make sysinstall
cd -
rm -rf /tmp/neon

# install 
cd mlp
python setup.py install
cd -

