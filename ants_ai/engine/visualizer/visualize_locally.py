#!/usr/bin/env python

import re
import sys
import os
# import webbrowser
import json
import shutil


def generate(data, generated_path):
    script_dir = os.path.dirname(__file__)
    template_path = os.path.join(script_dir, 'replay.html.template')
    template = open(template_path, 'r')
    content = template.read()
    template.close()

    path1 = os.path.realpath(__file__)
    path2 = os.path.realpath(generated_path)
    common = os.path.commonprefix((path1, path2))
    path1 = path1[len(common):]
    path2 = path2[len(common):]
    mod_path = '/'.join(['..'] * (path2.count(os.sep)) + [os.path.split(path1)[0].replace('\\', '/')])
    if len(mod_path) > 0 and mod_path[-1] != '/':
        mod_path += '/'

    quote_re = re.compile("'")
    newline_re = re.compile("\s", re.MULTILINE)
    insert_re = re.compile(r"## REPLAY PLACEHOLDER ##")

    try:
        json.loads(data)
        data = quote_re.sub(r"\\\\'", data)
        data = newline_re.sub("", data)
    except ValueError:
        data = data.replace('\n', '\\\\n')

    content = insert_re.sub(data, content)

    output = open(generated_path, 'w')
    output.write(content)
    output.close()

    visualizer_dir_name = os.path.join(os.path.dirname(generated_path), 'visualizer')
    if not os.path.exists(visualizer_dir_name):
        shutil.copytree(os.path.join(script_dir, 'data'), os.path.join(visualizer_dir_name, 'data'))
        shutil.copytree(os.path.join(script_dir, 'js'), os.path.join(visualizer_dir_name, 'js'))


def launch(filename=None, nolaunch=False, generated_path=None):
    if generated_path == None:
        generated_path = 'replay.html'
    if filename == None:
        data = sys.stdin.read()
        generated_path = os.path.realpath(os.path.join(os.path.dirname(__file__)
                                                       , generated_path))
    else:
        with open(filename, 'r') as f:
            data = f.read()
        generated_path = os.path.join(os.path.split(filename)[0], generated_path)
    generate(data, generated_path)


if __name__ == "__main__":
    launch(nolaunch=len(sys.argv) > 1 and sys.argv[1] == '--nolaunch')
