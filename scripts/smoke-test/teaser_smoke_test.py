import time
import argparse
from utils.android_view_helper import AndroidViewHelper
from utils.command_runner import CommandRunner
from utils.device_executor import DeviceExecutor

PROJECT_FOLDER = '../../'
BUILD_RELEASE_COMMAND = './gradlew clean :app:assembleRelease -PDISABLE_TELEMETRY=true'
TEASER_APK_FILE_PATH = PROJECT_FOLDER + 'app/build/outputs/apk/release/app-arm64-v8a-release.apk'
OUTPUT_FOLDER = PROJECT_FOLDER + 'app/build/outputs/smoke-test'
PACKAGE_NAME = 'com.mapbox.vision.teaser'
START_ACTIVITY_NAME = 'com.mapbox.vision.examples.activity.main.MainActivity'

AR_ROUTING_SCREEN = 'ar_routing'

SCREEN_ID_MAP = {
    'sign_detection': 'sign_detection_container',
    'segmentation': 'segm_container',
    'object_detection': 'det_container',
    'lane_detection': 'line_detection_container',
    'safety_mode': 'distance_container',
    AR_ROUTING_SCREEN: 'ar_navigation_button_container',
}

LOG_ADB_COMMANDS = True
LOG_ACTIONS = True
USE_LOG_CHECK = True  # Set False for LG G6
DEFAULT_TIME_BEFORE_SCREENSHOT_MS = 4000


def parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument("-b", dest="need_build_apk", help="branch for analysis", default=True,
                        type=lambda x: (str(x).lower() == 'true'))
    parser.add_argument("-t", dest="time_before_screenshot", help="file name with config",
                        default=DEFAULT_TIME_BEFORE_SCREENSHOT_MS)
    return parser.parse_args()


def log_message_to_console(message):
    if LOG_ACTIONS:
        print "*" * 80
        print '*   {:75}*'.format(message)
        print "*" * 80
        print "\n"


def build_release_apk(args):
    if args.need_build_apk:
        command_runner = CommandRunner(LOG_ADB_COMMANDS)
        log_message_to_console("Build Teaser release apk file")
        command_runner.execute_command_with_answer(BUILD_RELEASE_COMMAND, PROJECT_FOLDER)


def get_device_list():
    command_runner = CommandRunner(LOG_ADB_COMMANDS)
    str_ = command_runner.execute_command_with_answer('adb devices')
    _, _, devices_info = str_.partition('List of devices attached')
    devices_info = devices_info.replace('device', '').replace(' ', '').replace('\t', '')
    devices = devices_info.split('\n')
    devices = [x for x in devices if x != '']
    return devices


def open_screen_by_name(device_executor, screen_list, screen_name):
    positions = screen_list[screen_name]
    for index, position in enumerate(positions):

        if screen_name == AR_ROUTING_SCREEN and index == 2:
            device_executor.take_screenshot(OUTPUT_FOLDER, screen_name + '_map')

        device_executor.tap(position[0], position[1])
        sleep(position[2])

        if USE_LOG_CHECK and check_exception_in_logs(device_executor, screen_name):
            raise Exception('Exception in screen %s logs' % screen_name)


def sleep(time_ms):
    time.sleep(time_ms / 1000)


def open_all_screens(device_executor, screen_list, args):
    for screen_name in screen_list:
        log_message_to_console("Clear device logs for %s" % device_serial_name)
        device_executor.clear_device_logs()

        log_message_to_console("Open application for %s" % device_serial_name)
        device_executor.open_activity(PACKAGE_NAME, START_ACTIVITY_NAME)

        sleep(4000)
        log_message_to_console("Open screen %s for %s" % (screen_name, device_executor.device_serial_name))
        open_screen_by_name(device_executor, screen_list, screen_name)
        sleep(args.time_before_screenshot)
        device_executor.take_screenshot(OUTPUT_FOLDER, screen_name)
        sleep(2000)
        log_message_to_console("Close application for %s" % device_serial_name)
        device_executor.close_app(PACKAGE_NAME)


def check_exception_in_logs(device_executor, screen_name):
    sleep(1000)
    log_folder = '%s/%s/logs/' % (OUTPUT_FOLDER, device_executor.device_serial_name)
    log_file_name = 'logs-%s.txt' % (screen_name)

    device_executor.get_device_logs(log_folder, log_file_name, level='E')

    sleep(2000)
    with open(log_folder + log_file_name) as f:
        logs_from_device = f.readlines()
        for line in logs_from_device:
            if 'com.mapbox' in line and 'AndroidRuntime' in line:
                return True
    return False


def prepare_screen_coordinates(device_executor):
    view_folder = '%s/%s/views/' % (OUTPUT_FOLDER, device_executor.device_serial_name)
    view_file = 'view.xml'

    log_message_to_console("Open application for %s" % device_serial_name)
    device_executor.open_activity(PACKAGE_NAME, START_ACTIVITY_NAME)

    sleep(5000)

    device_executor.get_view_tree(view_folder, view_file)

    android_view_helper = AndroidViewHelper()
    size_x, size_y = android_view_helper.get_main_view_size(view_folder + view_file)
    screen_list = {}
    for resource_key, resource_value in SCREEN_ID_MAP.items():
        x, y = android_view_helper.get_view_center(view_folder + view_file,
                                                   'com.mapbox.vision.teaser:id/%s' % resource_value)
        screen_list[resource_key] = [[x, y, 0]]

    new_point_x = int(size_x * 0.7)
    new_point_y = int(size_y * 0.6)

    open_screen_by_name(device_executor, screen_list, AR_ROUTING_SCREEN)
    sleep(4000)
    device_executor.tap(new_point_x, new_point_y)
    sleep(3000)
    device_executor.get_view_tree(view_folder, view_file)

    map_go_x, map_go_y = android_view_helper.get_view_center(view_folder + view_file,
                                                             'com.mapbox.vision.teaser:id/start_ar')

    screen_list[AR_ROUTING_SCREEN][0][2] = 1000
    screen_list[AR_ROUTING_SCREEN].append([new_point_x, new_point_y, 2000])
    screen_list[AR_ROUTING_SCREEN].append([map_go_x, map_go_y, 0])

    device_executor.close_app(PACKAGE_NAME)
    return screen_list


if __name__ == '__main__':
    args = parse_args()
    build_release_apk(args)
    devices = get_device_list()

    for device_serial_name in devices:
        device_executor = DeviceExecutor(device_serial_name, LOG_ADB_COMMANDS)

        log_message_to_console("Uninstall apk for %s" % device_serial_name)
        device_executor.uninstall_apk(PACKAGE_NAME)

        log_message_to_console("Clear device logs for %s" % device_serial_name)
        device_executor.clear_device_logs()

        log_message_to_console("Install apk for %s" % device_serial_name)
        device_executor.install_apk(TEASER_APK_FILE_PATH, grant_all_permissions=True)

        screen_list = prepare_screen_coordinates(device_executor)

        open_all_screens(device_executor, screen_list, args)

        log_message_to_console("Uninstall apk for %s" % device_serial_name)
        device_executor.uninstall_apk(PACKAGE_NAME)
