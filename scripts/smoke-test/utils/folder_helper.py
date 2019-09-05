import os
import shutil


class FolderHelper:

    def __init__(self):
        pass

    def create_folders_if_not_exist(self, directories):
        for directory in directories:
            self.create_folder_if_not_exist(directory)

    @staticmethod
    def create_folder_if_not_exist(directory):
        if not os.path.exists(directory):
            os.makedirs(directory)

    @staticmethod
    def remove_folder_if_exist(directory):
        if os.path.exists(directory):
            shutil.rmtree(directory)
