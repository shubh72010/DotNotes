
def check_braces(filename):
    with open(filename, 'r', encoding='utf-8') as f:
        content = f.read()
    
    stack = []
    for i, char in enumerate(content):
        if char == '{':
            # find line number
            line_num = content[:i].count('\n') + 1
            stack.append(('{', line_num))
        elif char == '}':
            if not stack:
                line_num = content[:i].count('\n') + 1
                return f"Unexpected '}}' at line {line_num}"
            stack.pop()
    
    if stack:
        char, line_num = stack[-1]
        return f"Unclosed '{char}' at line {line_num}"
    
    return "Braces are balanced"

import sys
if len(sys.argv) > 1:
    print(check_braces(sys.argv[1]))
else:
    print("Usage: python check_braces.py <filename>")
