from pathlib import Path
p = Path('app/src/main/java/com/aarvo/MainActivity.kt')
s = p.read_text()
if 'import androidx.compose.foundation.layout.size' not in s:
    s = s.replace('import androidx.compose.foundation.layout.padding\n', 'import androidx.compose.foundation.layout.padding\nimport androidx.compose.foundation.layout.size\n', 1)
p.write_text(s)
