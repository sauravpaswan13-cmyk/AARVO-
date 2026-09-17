from pathlib import Path
import re

p = Path('app/src/main/java/com/aarvo/MainActivity.kt')
s = p.read_text(encoding='utf-8')

old_call = '!onboarded -> OnboardingScreen { prefs.edit().putBoolean("onboarded", true).apply(); onboarded = true }'
new_call = '!onboarded -> OnboardingScreen(onDone = { prefs.edit().putBoolean("onboarded", true).apply(); onboarded = true }, onGuest = { prefs.edit().putBoolean("onboarded", true).putBoolean("guest_mode", true).apply(); onboarded = true; guestMode = true })'
s = s.replace(old_call, new_call)

old_fn = '''@Composable private fun OnboardingScreen(onDone: () -> Unit) { Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) { Text("AARVO", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold); Spacer(Modifier.height(12.dp)); Text("Shop smart. Live better.", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp)); Text("A real marketplace for buyers and sellers, with server-authoritative products, orders and payments."); Spacer(Modifier.height(24.dp)); Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Get started") } } }'''
new_fn = '''@Composable private fun OnboardingScreen(onDone: () -> Unit, onGuest: () -> Unit) { Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) { Text("AARVO", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold); Spacer(Modifier.height(12.dp)); Text("Shop smart. Live better.", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp)); Text("Browse products freely or continue with your AARVO account."); Spacer(Modifier.height(24.dp)); Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Login / Continue") }; Spacer(Modifier.height(10.dp)); OutlinedButton(onClick = onGuest, modifier = Modifier.fillMaxWidth()) { Text("Continue as Guest") } } }'''
s = s.replace(old_fn, new_fn)

# Remove the old search field from its lower position.
s = s.replace('item { OutlinedTextField(query, onQueryChange, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Search for products, brands and more...") }) }', '')

# Make the category row visual/image-led while remaining offline-safe.
old_art = 'Text(if (item == "All") "🛍️" else "🛒", fontSize = 25.sp)'
new_art = '''Card(shape = RoundedCornerShape(16.dp)) { Text(when (item.lowercase()) { "all" -> "🛍️"; "fashion", "clothing", "apparel" -> "👕"; "electronics", "mobile", "mobiles" -> "📱"; "home", "home & kitchen", "kitchen" -> "🏠"; "beauty", "personal care" -> "💄"; "grocery", "groceries" -> "🛒"; "sports" -> "⚽"; "books" -> "📚"; "toys" -> "🧸"; else -> "🛍️" }, fontSize = 38.sp, modifier = Modifier.padding(9.dp)) }'''
s = s.replace(old_art, new_art)
s = s.replace('Modifier.padding(horizontal = 14.dp, vertical = 10.dp)', 'Modifier.width(92.dp).padding(horizontal = 8.dp, vertical = 10.dp)')

# Search must be the first item in Home. Existing build-time hero injection is reused.
marker = 'item { PremiumHomeHeader() }; item { LiveHero(api = AarvoApiClient(), modifier = Modifier.fillMaxWidth()) };'
replacement = 'item { HomeSearchFirst(query, onQueryChange) }; item { PremiumHomeHeader() }; item { LiveHero(api = AarvoApiClient(), modifier = Modifier.fillMaxWidth()) };'
s = s.replace(marker, replacement, 1)

# Add a small search composable once, before LoginRequiredDialog.
if 'private fun HomeSearchFirst(' not in s:
    helper = '''@Composable private fun HomeSearchFirst(query: String, onQueryChange: (String) -> Unit) { OutlinedTextField(query, onQueryChange, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Search for products, brands and more...") }) }\n\n'''
    s = s.replace('@Composable private fun LoginRequiredDialog', helper + '@Composable private fun LoginRequiredDialog', 1)

p.write_text(s, encoding='utf-8')
print('AARVO guest + search-first + visual-category UI applied')
