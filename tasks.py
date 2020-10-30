from invoke import task
import os
sep = '&' if os.name == 'nt' else ';'

@task
def test(context):
    context.run(f'cd ants_ai {sep} python -m unittest')

@task
def lint(context):
    context.run('pylint ants_ai')


@task
def build_java_bots(context):
    context.run(f'cd bots {sep} ant clean {sep} ant compile {sep} ant jar')

@task
def run_data_gen(context):
    context.run(f'java -jar {os.path.abspath("./bots/build/jar/bots.jar")}', asynchronous=True)
    context.run(f'python {os.path.abspath("./ants_ai/training/data_gen/__main__.py")}')

@task
def run_neural_net_bot(context):
    context.run(f'java -jar {os.path.abspath("./bots/build/jar/bots.jar")}', asynchronous=True)
    context.run(f'python {os.path.abspath("./ants_ai/neural_network_bot/__main__.py")}')

@task
def run_training(context):
    context.run(f'cd ants_ai {sep} python training/neural_network/__main__.py')

@task
def run_training_cpu(context):
    context.run(f'CALL SET CUDA_VISIBLE_DEVICES=-1 {sep} cd ants_ai {sep} python training/neural_network/__main__.py')

@task
def start_tensor_board(context):
    context.run('tensorboard --logdir ants_ai/logs/fit')

@task
def start_profiler(context):
    context.run('snakeviz ants_example.profile')
