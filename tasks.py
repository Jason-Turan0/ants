from invoke import task, run
import os
@task
def test(context):
    context.run('cd ants_ai & python -m unittest')

@task
def buildJavaBots(context):
    context.run('cd bots & ant clean & ant compile & ant jar')

@task
def runTournament(context):
    context.run(f'java -jar {os.getcwd()}/bots/build/jar/bots.jar', asynchronous=True)
    context.run(f'python {os.getcwd()}/ants_ai/training/data_gen/__main__.py')

@task
def runNeuralNetBot(context):
    context.run(f'java -jar {os.getcwd()}/bots/build/jar/bots.jar', asynchronous=True)
    context.run(f'python {os.getcwd()}/ants_ai/neural_network_bot/__main__.py')

@task
def runTraining(context):
    context.run('cd ants_ai & python training/neural_network/__main__.py')

@task
def startTensorBoard(context):
    context.run('tensorboard --logdir logs/fit')

@task
def startProfiler(context):
    context.run('snakeviz nn_game.profile')
