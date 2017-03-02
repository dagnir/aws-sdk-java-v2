class ReleaseChanges(object):
    def __init__(self, version, date, entries):
        self.version = version
        self.date = date
        self.entries = entries

class ChangelogEntry(object):
    def __init__(self, type, category, description):
        self.type = type
        self.category = category
        self.description = description

class Version(object):
    def __init__(self, major, minor, patch):
        self.major = major
        self.minor = minor
        self.patch = patch

    def __str__(self):
        return "%d.%d.%d" % (self.major, self.minor, self.patch)
