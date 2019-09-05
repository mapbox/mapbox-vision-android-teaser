import xml.etree.ElementTree as ET


class AndroidViewHelper:

    def __init__(self):
        pass

    def get_main_view_size(self, view_tree_path):
        x1, y1, x2, y2 = self.get_view_bounds(view_tree_path, '')
        return x2, y2

    def get_view_center(self, view_tree_path, resource_id):
        x1, y1, x2, y2 = self.get_view_bounds(view_tree_path, resource_id)
        return (x1 + x2) // 2, (y1 + y2) // 2

    def get_view_bounds(self, view_tree_path, resource_id):
        root = ET.parse(view_tree_path).getroot()
        nodes = root.findall(".//node[@resource-id='%s']" % resource_id)
        if len(nodes) == 0:
            raise Exception('Can\'t find view with id <%s>' % resource_id)
        return self._get_view_bounds(nodes[0])

    def _get_view_bounds(self, node):
        raw_bounds = node.attrib['bounds']
        raw_bounds = raw_bounds.replace('][', ',').replace('[', '').replace(']', '')
        raw_bounds = raw_bounds.split(',')
        bounds = [int(i) for i in raw_bounds]
        return bounds[0], bounds[1], bounds[2], bounds[3]
