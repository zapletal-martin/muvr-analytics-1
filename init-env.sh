#!/bin/bash
set -xe

# Install pip
if ! [ `command -v pip` ]
then
  sudo easy_install pip
fi

# Create virtual env
VENV=venv
pip install virtualenv
rm -rf $VENV
virtualenv $VENV -p /usr/bin/python2.7
source $VENV/bin/activate

# Install dependencies
pip install --download-cache=cache -r muvr.pip

# Insteall neon latest
git clone https://github.com/NervanaSystems/neon.git $VENV/neon
cd $VENV/neon
make sysinstall
cd -

# Install mlp
cd mlp
python setup.py install
cd -

# Install Spark
cd $VENV
wget http://www.eu.apache.org/dist/spark/spark-1.5.1/spark-1.5.1-bin-hadoop2.6.tgz
if ! [ `command -v pv` ]
then
  brew install pv
fi
[ pv spark-1.5.1-bin-hadoop2.6.tgz | tar xzf - ] || tar -xzf spark-1.5.1-bin-hadoop2.6.tgz
cd -
cat >> $VENV/bin/activate << EOF
export SPARK_HOME=`pwd`/$VENV/spark-1.5.1-bin-hadoop2.6
export PYTHONPATH=$SPARK_HOME/python:$SPARK_HOME/python/build:$PYTHONPATH
PATH=$SPARK_HOME/bin:$PATH
EOF

