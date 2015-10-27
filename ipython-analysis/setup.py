try:
    from setuptools import setup
except ImportError:
    from distutils.core import setup

setup(
    name='muvr-analysis-notebooks',
    version='1.0',
    url='https://github.com/muvr/muvr-analytics',
    license='',
    author='Tom Bocklisch',
    author_email='tomb@cakesolutions.net',
    description='Ipython notebook analysis for the muvr project',
    requires=['ipy_table']
)
