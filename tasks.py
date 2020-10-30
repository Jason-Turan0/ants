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

@task(help={'save_path': "Directory to save replay data to"})
def run_data_gen(context, save_path, game_count):
    context.run(f'java -jar {os.getcwd()}/bots/build/jar/bots.jar', asynchronous=True)
    context.run(f'python {os.getcwd()}/ants_ai/training/data_gen/__main__.py -sp "{save_path}" -gc {game_count}')

@task(help={'save_path': "Directory to save replay data to"})
def run_neural_net_bot(context, save_path):
    context.run(f'java -jar {os.getcwd()}/bots/build/jar/bots.jar', asynchronous=True)
    context.run(f'python {os.getcwd()}/ants_ai/neural_network_bot/__main__.py -sp {save_path}')

@task(help={'data_path': "The root folder to look for training data and to save training stats"})
def run_training(context, data_path):
    context.run(f'cd ants_ai & python training/neural_network/__main__.py -dp {data_path}')

@task(help={'data_path': "The root folder to look for training data and to save training stats"})
def run_training_cpu(context, data_path):
    context.run('CALL SET CUDA_VISIBLE_DEVICES=-1 & cd ants_ai & python training/neural_network/__main__.py')

@task(help={'data_path': "The root folder to look for training stats"})
def start_tensor_board(context, data_path):
    context.run(f'tensorboard --logdir {data_path}/logs/fit')

@task
def start_profiler(context):
    context.run('snakeviz ants_example.profile')
