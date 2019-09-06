from utils.command_runner import CommandRunner
from utils.folder_helper import FolderHelper
import time


class DeviceExecutor:

    def __init__(self, device_serial_name, log_adb_commands=False):
        self._command_runner = CommandRunner(log_adb_commands)
        self._folder_helper = FolderHelper()
        self.device_serial_name = device_serial_name

    def uninstall_apk(self, package_name):
        self._command_runner.execute_command(
            'adb -s %s shell pm uninstall -k %s' % (self.device_serial_name, package_name))

    def install_apk(self, apk_path, grant_all_permissions=False):
        grant_value = ''
        if grant_all_permissions:
            grant_value = ' -g'
        self._command_runner.execute_command(
            'adb -s %s install%s %s' % (self.device_serial_name, grant_value, apk_path))

    def close_app(self, package_name):
        self._command_runner.execute_command(
            'adb -s %s shell am force-stop %s' % (self.device_serial_name, package_name))

    def tap(self, x, y):
        self._command_runner.execute_command_with_answer(
            'adb -s %s shell input tap %d %d' % (self.device_serial_name, x, y))

    def open_activity(self, package_name, activity_name, params=""):
        self._command_runner.execute_command(
            'adb -s %s shell am start -n %s/%s %s' % (self.device_serial_name, package_name, activity_name, params))

    def get_view_tree(self, view_tree_path, view_tree_file):
        self._folder_helper.create_folder_if_not_exist(view_tree_path)
        self._command_runner.execute_command_with_answer(
            'adb -s %s shell uiautomator dump' % self.device_serial_name)
        time.sleep(2)
        self._command_runner.execute_command_with_answer(
            'adb -s %s pull /sdcard/window_dump.xml %s' % (self.device_serial_name, view_tree_path + view_tree_file))

    def clear_device_logs(self):
        self._command_runner.execute_command_with_answer(
            'adb -s %s logcat -c' % self.device_serial_name)

    def get_device_logs(self, log_path, log_file, level='D'):
        self._command_runner.execute_command_with_answer(
            'adb -s %s logcat *:%s -d -f /sdcard/smoke_logs.txt' % (self.device_serial_name, level))
        self._folder_helper.create_folder_if_not_exist(log_path)
        self._command_runner.execute_command_with_answer(
            'adb -s %s pull /sdcard/smoke_logs.txt %s' % (self.device_serial_name, log_path + log_file))

    def take_screenshot(self, output_path, scree_name):
        self._command_runner.execute_command_with_answer(
            'adb -s %s shell /system/bin/screencap -p /sdcard/smoke_screenshot.png' % self.device_serial_name)
        self._folder_helper.create_folder_if_not_exist(
            '%s/%s/images/' % (output_path, self.device_serial_name))
        self._command_runner.execute_command_with_answer(
            'adb -s %s pull /sdcard/smoke_screenshot.png %s/%s/images/%s.png' % (
                self.device_serial_name, output_path, self.device_serial_name, scree_name))
        self._command_runner.execute_command(
            'adb -s %s shell rm /sdcard/smoke_screenshot.png' % self.device_serial_name)
