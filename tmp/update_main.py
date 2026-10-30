import re

path = '/tmp/aarvo_repo/app/src/main/java/com/aarvo/MainActivity.kt'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Replace TopAppBar in Scaffold with full royal blue header containing search bar
pattern_top_bar = r'topBar = \{\s+TopAppBar\(.*?\)\s+\},'
replacement_top_bar = '''topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2418D8))
                        .padding(bottom = 12.dp)
                ) {
                    // Top Bar: Hamburger Menu, AARVO Brand Logo + Name, Notifications and Cart
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, "Menu", tint = Color.White)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { selectedTab = 0 }
                        ) {
                            androidx.compose.foundation.Image(
                                painter = coil3.compose.rememberAsyncImagePainter(model = R.drawable.aarvo_brand_logo),
                                contentDescription = "AARVO logo",
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "AARVO",
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                fontSize = 22.sp
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (unreadNotificationCount > 0) {
                                BadgedBox(badge = { Badge { Text(unreadNotificationCount.toString()) } }) {
                                    IconButton(onClick = { showNotifications = true }) {
                                        Icon(Icons.Default.Notifications, "Notifications", tint = Color.White)
                                    }
                                }
                            } else {
                                IconButton(onClick = { showNotifications = true }) {
                                    Icon(Icons.Default.Notifications, "Notifications", tint = Color.White)
                                }
                            }
                            if (cartBadgeCount > 0) {
                                BadgedBox(badge = { Badge { Text(cartBadgeCount.toString()) } }) {
                                    IconButton(onClick = { selectedTab = 3 }) {
                                        Icon(Icons.Default.ShoppingCart, "Cart", tint = Color.White)
                                    }
                                }
                            } else {
                                IconButton(onClick = { selectedTab = 3 }) {
                                    Icon(Icons.Default.ShoppingCart, "Cart", tint = Color.White)
                                }
                            }
                        }
                    }

                    // Bada Search Bar inside the Blue Screen matching user's exact design
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                            .height(48.dp),
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                if (query.isEmpty()) {
                                    Text(
                                        "Search for products, brands and more...",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 13.sp,
                                        maxLines = 1
                                    )
                                }
                                androidx.compose.foundation.text.BasicTextField(
                                    value = query,
                                    onValueChange = {
                                        query = it
                                        if (selectedTab != 0 && selectedTab != 1) selectedTab = 0
                                    },
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        color = Color(0xFF1E293B),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = "" }, modifier = Modifier.size(24.dp)) {
                                    Text("✕", fontSize = 12.sp, color = Color(0xFF94A3B8))
                                }
                            }
                            Text(
                                "🎙️",
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            },'''

new_content, count = re.subn(pattern_top_bar, replacement_top_bar, content, count=1, flags=re.DOTALL)
if count > 0:
    print("1. Top bar replaced successfully")
    content = new_content
else:
    print("1. Top bar regex match failed")

# 2. Update navigation item colors
old_nav_colors = 'selectedIconColor = Color(0xFF4B16D8), selectedTextColor = Color(0xFF4B16D8), indicatorColor = Color(0xFFEDE9FE)'
new_nav_colors = 'selectedIconColor = Color(0xFF2418D8), selectedTextColor = Color(0xFF2418D8), unselectedIconColor = Color(0xFF64748B), unselectedTextColor = Color(0xFF64748B), indicatorColor = Color.Transparent'
if old_nav_colors in content:
    content = content.replace(old_nav_colors, new_nav_colors)
    print("2. Navigation item colors updated")

# 3. In HomeScreen, remove duplicate HomeSearchFirst inside LazyColumn
pattern_home_search = r'item \{\s*HomeSearchFirst\(query,\s*onQueryChange\)\s*\}'
new_content, count = re.subn(pattern_home_search, '// Search bar moved to unified Blue Header\n        item { Spacer(Modifier.height(2.dp)) }', content, count=1)
if count > 0:
    print("3. Duplicate search bar removed from HomeScreen")
    content = new_content

# 4. Update Top Deals For You Banner in HomeScreen to Royal Blue Color(0xFF2418D8)
content = re.sub(
    r'// 4\. Top Deals For You Banner\s+item \{\s+Card\(\s+modifier = Modifier\.fillMaxWidth\(\)\.padding\(vertical = 4\.dp\)\.clickable \{ onOpenOffers\(\) \},\s+shape = RoundedCornerShape\(18\.dp\),\s+colors = CardDefaults\.cardColors\(containerColor = Color\(0xFFFF7A00\)\)',
    '''// 4. Top Deals For You Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onOpenOffers() },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2418D8))''',
    content
)
print("4. Top Deals banner color updated")

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
print("All updates written to MainActivity.kt!")
