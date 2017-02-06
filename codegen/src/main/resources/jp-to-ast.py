#!/usr/bin/env python
import json
import sys

import jmespath


expression = sys.argv[1]
parsed = jmespath.compile(expression)
print(json.dumps(parsed.parsed, indent = 2))