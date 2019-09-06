import shlex
import subprocess


class CommandRunner:

    def __init__(self, log_commands=False):
        self._log_commands = log_commands

    def execute_command(self, cmd, cwd=None):
        try:
            subprocess_cmd = shlex.split(cmd)
            if self._log_commands:
                print cmd
            p = subprocess.Popen(subprocess_cmd, cwd=cwd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
            p.communicate()
            p.wait()
        except Exception as e:
            print e.message

    def execute_command_with_answer(self, cmd, cwd=None):
        try:
            if self._log_commands:
                print cmd
            subprocess_cmd = shlex.split(cmd)
            s = subprocess.check_output(subprocess_cmd, cwd=cwd)
            return s
        except Exception as e:
            print e.message
