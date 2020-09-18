from invoke import task
import os

@task
def test(context):
    context.run('cd ants_ai & python -m unittest')

@task
def lint(context):
    context.run('pylint ants_ai')


@task
def build_java_bots(context):
    context.run('cd bots & ant clean & ant compile & ant jar')

@task
def run_tournament(context):
    context.run(f'java -jar {os.getcwd()}/bots/build/jar/bots.jar', asynchronous=True)
    context.run(f'python {os.getcwd()}/ants_ai/training/data_gen/__main__.py')

@task
def run_neural_net_bot(context):
    context.run(f'java -jar {os.getcwd()}/bots/build/jar/bots.jar', asynchronous=True)
    context.run(f'python {os.getcwd()}/ants_ai/neural_network_bot/__main__.py')

@task
def run_training(context):
    context.run('cd ants_ai & python training/neural_network/__main__.py')

@task
def start_tensor_board(context):
    context.run('tensorboard --logdir logs/fit')

@task
def start_profiler(context):
    context.run('snakeviz nn_game.profile')
