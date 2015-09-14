try:
    from setuptools import setup
except ImportError:
    from distutils.core import setup

setup(
    name='muvr-analysis-mlp',
    version='1.0-SNAPSHOT',
    packages=['converters', 'training'],
    url='https://github.com/muvr/muvr-analytics',
    license='',
    author='Tom Bocklisch',
    author_email='tomb@cakesolutions.net',
    description='Spark analysis for the muvr project',
    requires=['numpy', 'neon']
)
