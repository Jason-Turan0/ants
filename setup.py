from setuptools import setup, find_packages
with open('ants_ai/README.rst') as f:
    readme = f.read()
with open('ants_ai/LICENSE') as f:
    module_license = f.read()

setup(
    name='ants.ai',
    version='0.1.0',
    description='Machine learning project to emulate hand written programs submitted for Google Ants AI Challenge 2011 working backwards from examples of expert play and Machine Learning techniques.',
    long_description=readme,
    author='Jason Turan',
    author_email='turan.jason@gmail.com',
    url='https://github.com/Jason-Turan0/ants',
    license=module_license,
    packages=find_packages(exclude=('tests', 'docs')),
    install_requires=[
        'py4j', 'PyFunctional', 'jsonpickle', 'snakeviz',
        'sklearn', 'tensorflow==2.1.1', 'keras-tuner', 'pandas', 'pydot',
        'graphviz', 'matplotlib', 'invoke', 'pylint', 'PrettyTable'
    ]
)