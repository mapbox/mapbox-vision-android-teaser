import os
import shutil


class FolderHelper:

    def __init__(self):
        pass

    @staticmethod
    def create_folders_if_not_exist(directories):
        for directory in directories:
            FolderHelper.create_folder_if_not_exist(directory)

    @staticmethod
    def create_folder_if_not_exist(directory):
        if not os.path.exists(directory):
            os.makedirs(directory)

    @staticmethod
    def remove_folder_if_exist(directory):
        if os.path.exists(directory):
            shutil.rmtree(directory)
