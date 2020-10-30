from invoke import task
import os
sep = '&' if os.name == 'nt' else ';'

def format_arg(prefix, value):
    return f'{prefix} {value}' if value is not None else ""

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
def run_data_gen(context, save_path= None, game_count = None):
    context.run(f'java -jar {os.path.abspath("./bots/build/jar/bots.jar")}', asynchronous=True)
    context.run(f'python {os.path.abspath("./ants_ai/training/data_gen/__main__.py")} {format_arg("-sp", save_path)} {format_arg("-gc", game_count)}')

@task(help={
    'save_path': "Directory to save replay data to",
    'model_path': "Path to trained model"
})
def run_neural_net_bot(context, save_path=None, model_path=None):
    context.run(f'java -jar {os.path.abspath("./bots/build/jar/bots.jar")}', asynchronous=True)
    context.run(f'python {os.path.abspath("./ants_ai/neural_network_bot/__main__.py")} {format_arg("-sp", save_path)} {format_arg("-mp", model_path)}')

@task(help={'data_path': "The root folder to look for training data and to save training stats"})
def run_training(context, data_path=None):
    context.run(f'cd ants_ai {sep} python {os.path.abspath("./ants_ai/training/neural_network/__main__.py")}  {format_arg("-dp", data_path)}')

@task(help={'data_path': "The root folder to look for training data and to save training stats"})
def run_training_cpu(context, data_path=None):
    set_global_env = 'CALL SET CUDA_VISIBLE_DEVICES=-1' if os.name == 'nt' else 'CUDA_VISIBLE_DEVICES=-1'
    context.run(f'{set_global_env} {sep} cd ants_ai {sep} python {os.path.abspath("./ants_ai/training/neural_network/__main__.py")}  {format_arg("-dp", data_path)}')

@task(help={'data_path': "The root folder to look for training stats"})
def start_tensor_board(context, data_path= None):
    context.run(f'tensorboard --logdir {os.path.abspath("./ants_ai_data/logs/fit") if data_path is None else data_path}')

@task
def start_profiler(context):
    context.run('snakeviz ants_example.profile')
