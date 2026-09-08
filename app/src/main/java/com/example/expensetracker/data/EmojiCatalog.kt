package com.example.expensetracker.data

/**
 * The emoji offered when you pick an icon for a category, each with the words
 * you might go looking for it by.
 *
 * Nothing here is newer than Emoji 11.0 (2018). Later emoji render as an empty
 * box on the older devices this app still supports, and an icon you cannot see
 * is worse than one you never picked.
 */
object EmojiCatalog {

    data class Entry(val emoji: String, val keywords: List<String>)

    /** The icon a brand new category starts on. */
    const val DEFAULT = "📌"

    private fun entry(emoji: String, vararg keywords: String) = Entry(emoji, keywords.toList())

    val entries: List<Entry> = listOf(
        // Eating out
        entry("🍔", "burger", "fast food", "food"),
        entry("🍕", "pizza", "food"),
        entry("🌭", "hotdog", "food"),
        entry("🥪", "sandwich", "food"),
        entry("🌮", "taco", "mexican", "food"),
        entry("🌯", "burrito", "wrap", "roll", "food"),
        entry("🥗", "salad", "healthy", "food"),
        entry("🍜", "noodles", "ramen", "soup", "food"),
        entry("🍝", "pasta", "spaghetti", "food"),
        entry("🍛", "curry", "rice", "food"),
        entry("🍚", "rice", "food"),
        entry("🍲", "stew", "pot", "food"),
        entry("🥘", "pan", "cooking", "food"),
        entry("🍱", "bento", "lunch", "tiffin", "food"),
        entry("🍣", "sushi", "food"),
        entry("🍤", "prawn", "shrimp", "fry", "food"),
        entry("🍗", "chicken", "meat", "food"),
        entry("🥩", "steak", "meat", "food"),
        entry("🥚", "egg", "food"),
        entry("🥞", "pancake", "breakfast", "food"),
        entry("🍞", "bread", "bakery", "food"),
        entry("🥐", "croissant", "bakery", "food"),
        entry("🧀", "cheese", "dairy", "food"),
        entry("🥛", "milk", "dairy", "food"),

        // Drinks
        entry("☕", "coffee", "cafe", "drink"),
        entry("🍵", "tea", "chai", "drink"),
        entry("🧃", "juice", "drink"),
        entry("🥤", "soda", "soft drink", "cold drink", "drink"),
        entry("🍺", "beer", "pub", "bar", "drink"),
        entry("🍷", "wine", "bar", "drink"),
        entry("🍸", "cocktail", "bar", "drink"),

        // Sweets and snacks
        entry("🍰", "cake", "dessert", "sweet"),
        entry("🎂", "birthday", "cake", "celebration"),
        entry("🍩", "donut", "dessert", "sweet"),
        entry("🍪", "cookie", "biscuit", "snack"),
        entry("🍫", "chocolate", "candy", "sweet"),
        entry("🍦", "icecream", "ice cream", "dessert"),
        entry("🍿", "popcorn", "snack", "cinema"),

        // Groceries
        entry("🛒", "cart", "grocery", "groceries", "supermarket", "shopping"),
        entry("🍎", "apple", "fruit", "grocery"),
        entry("🍌", "banana", "fruit", "grocery"),
        entry("🍇", "grapes", "fruit", "grocery"),
        entry("🍉", "watermelon", "fruit", "grocery"),
        entry("🥭", "mango", "fruit", "grocery"),
        entry("🥑", "avocado", "fruit", "grocery"),
        entry("🥕", "carrot", "vegetable", "grocery"),
        entry("🌽", "corn", "vegetable", "grocery"),
        entry("🍅", "tomato", "vegetable", "grocery"),
        entry("🥔", "potato", "vegetable", "grocery"),
        entry("🧅", "onion", "vegetable", "grocery"),
        entry("🧄", "garlic", "vegetable", "grocery"),

        // Shopping
        entry("🛍️", "shopping", "bags", "retail"),
        entry("🏪", "store", "shop", "convenience"),
        entry("🏬", "mall", "department store", "shopping"),
        entry("🎁", "gift", "present"),
        entry("🏷️", "price", "tag", "sale", "discount"),
        entry("💳", "card", "credit", "debit", "payment"),
        entry("🧾", "receipt", "bill", "invoice"),

        // Getting around
        entry("🚗", "car", "drive", "transport"),
        entry("🚕", "taxi", "cab", "auto", "rickshaw", "transport"),
        entry("🚙", "suv", "car", "transport"),
        entry("🚌", "bus", "transport"),
        entry("🚐", "van", "minibus", "transport"),
        entry("🏍️", "motorcycle", "bike", "transport"),
        entry("🛵", "scooter", "moped", "transport"),
        entry("🚲", "bicycle", "cycle", "transport"),
        entry("🛴", "kick scooter", "transport"),
        entry("🚉", "station", "train", "transport"),
        entry("🚆", "train", "rail", "transport"),
        entry("🚇", "metro", "subway", "underground", "transport"),
        entry("🚊", "tram", "transport"),
        entry("⛽", "fuel", "petrol", "diesel", "gas", "transport"),
        entry("🅿️", "parking", "transport"),
        entry("🛣️", "highway", "road", "toll", "transport"),

        // Travel
        entry("✈️", "plane", "flight", "travel"),
        entry("🛫", "takeoff", "departure", "flight", "travel"),
        entry("🛬", "landing", "arrival", "flight", "travel"),
        entry("🚢", "ship", "cruise", "travel"),
        entry("⛴️", "ferry", "boat", "travel"),
        entry("🧳", "luggage", "suitcase", "travel"),
        entry("🗺️", "map", "travel"),
        entry("🧭", "compass", "navigation", "travel"),
        entry("🏨", "hotel", "stay", "travel"),
        entry("⛺", "camping", "tent", "travel"),
        entry("🏖️", "beach", "holiday", "vacation", "travel"),

        // Home
        entry("🏠", "home", "house", "rent"),
        entry("🏡", "house", "garden", "home"),
        entry("🏢", "apartment", "office", "building"),
        entry("🏗️", "construction", "renovation", "repair"),
        entry("🔑", "key", "rent", "deposit"),
        entry("🛏️", "bed", "bedroom", "furniture"),
        entry("🛋️", "sofa", "couch", "furniture"),
        entry("🚿", "shower", "bathroom"),
        entry("🚽", "toilet", "bathroom"),
        entry("🚪", "door", "home"),
        entry("🌱", "seedling", "garden", "plant"),
        entry("🌿", "herb", "plant", "garden"),

        // Bills and utilities
        entry("⚡", "electricity", "power", "bill"),
        entry("💡", "light", "bulb", "electricity", "bill"),
        entry("💧", "water", "bill"),
        entry("🔥", "gas", "heating", "bill"),
        entry("🌡️", "heating", "temperature"),
        entry("❄️", "cooling", "ac", "air conditioner"),

        // Cleaning and repairs
        entry("🧹", "broom", "cleaning", "household"),
        entry("🧽", "sponge", "cleaning", "household"),
        entry("🧼", "soap", "cleaning", "household"),
        entry("🧺", "laundry", "washing", "household"),
        entry("🧻", "tissue", "toilet paper", "household"),
        entry("🔧", "wrench", "repair", "maintenance"),
        entry("🔨", "hammer", "repair", "diy"),
        entry("🧰", "toolbox", "repair", "maintenance"),

        // Phone, internet and tech
        entry("📱", "phone", "mobile", "recharge"),
        entry("📞", "call", "telephone"),
        entry("☎️", "landline", "phone"),
        entry("📶", "signal", "network", "data", "internet"),
        entry("💻", "laptop", "computer"),
        entry("🖥️", "desktop", "monitor", "computer"),
        entry("⌨️", "keyboard", "computer"),
        entry("🖱️", "mouse", "computer"),
        entry("🖨️", "printer", "printing"),
        entry("🔌", "plug", "charger", "electricity"),
        entry("🔋", "battery", "power"),
        entry("💾", "storage", "backup", "disk"),
        entry("🎧", "headphones", "audio"),
        entry("📷", "camera", "photo"),
        entry("📺", "tv", "television", "subscription"),
        entry("📻", "radio", "audio"),

        // Health
        entry("💊", "medicine", "pill", "pharmacy", "chemist", "health"),
        entry("💉", "injection", "vaccine", "health"),
        entry("🏥", "hospital", "clinic", "doctor", "health"),
        entry("🦷", "dentist", "teeth", "health"),
        entry("👓", "glasses", "spectacles", "optician", "health"),
        entry("🧬", "lab", "test", "health"),
        entry("🧪", "lab", "test tube", "health"),
        entry("🧠", "brain", "therapy", "mental health"),
        entry("❤️", "heart", "health", "love", "charity"),

        // Fitness
        entry("💪", "gym", "fitness", "workout"),
        entry("🏋️", "weights", "gym", "lifting"),
        entry("🧘", "yoga", "meditation"),
        entry("🏃", "running", "exercise"),
        entry("🚴", "cycling", "exercise"),
        entry("🏊", "swimming", "pool", "exercise"),
        entry("⚽", "football", "soccer", "sport"),
        entry("🏏", "cricket", "sport"),
        entry("🏸", "badminton", "sport"),
        entry("🎾", "tennis", "sport"),
        entry("🥋", "martial arts", "karate", "sport"),

        // Looking after yourself
        entry("💇", "haircut", "salon", "barber"),
        entry("💅", "nails", "manicure", "salon"),
        entry("💄", "makeup", "cosmetics"),
        entry("🧴", "lotion", "skincare"),
        entry("🧖", "spa", "sauna", "massage"),

        // Clothes
        entry("👗", "dress", "clothes"),
        entry("👕", "shirt", "tshirt", "clothes"),
        entry("👖", "jeans", "trousers", "clothes"),
        entry("👔", "formal", "tie", "office", "clothes"),
        entry("👞", "shoes", "formal", "footwear"),
        entry("👟", "sneakers", "shoes", "footwear"),
        entry("👠", "heels", "shoes", "footwear"),
        entry("🧥", "coat", "jacket", "clothes"),
        entry("🧦", "socks", "clothes"),
        entry("🧢", "cap", "hat", "clothes"),
        entry("👜", "handbag", "purse", "bag"),
        entry("🎒", "backpack", "bag", "school"),
        entry("⌚", "watch", "wearable"),
        entry("💍", "ring", "jewellery", "jewelry"),
        entry("📿", "necklace", "beads", "jewellery"),

        // Going out
        entry("🎬", "movie", "cinema", "film"),
        entry("🎟️", "ticket", "event", "entry"),
        entry("🎪", "circus", "show", "event"),
        entry("🎭", "theatre", "drama", "play"),
        entry("🎨", "art", "painting", "hobby"),
        entry("🎵", "music", "song"),
        entry("🎸", "guitar", "music", "instrument"),
        entry("🎹", "piano", "music", "instrument"),
        entry("🥁", "drums", "music", "instrument"),
        entry("🎤", "mic", "karaoke", "singing"),
        entry("🎮", "gaming", "console", "video game"),
        entry("🕹️", "arcade", "joystick", "game"),
        entry("🎲", "board game", "dice", "game"),
        entry("🃏", "cards", "poker", "game"),
        entry("🧩", "puzzle", "hobby"),
        entry("🎉", "party", "celebration"),
        entry("🎊", "confetti", "celebration"),

        // Learning
        entry("📚", "books", "reading", "education"),
        entry("📖", "book", "reading"),
        entry("📰", "news", "newspaper", "subscription"),
        entry("🎓", "education", "college", "graduation", "fees"),
        entry("✏️", "pencil", "stationery", "school"),
        entry("🖊️", "pen", "stationery"),
        entry("📝", "notes", "notebook", "stationery"),
        entry("📐", "geometry", "stationery", "school"),

        // Family and pets
        entry("👶", "baby", "childcare"),
        entry("🧒", "child", "kids"),
        entry("👪", "family", "kids"),
        entry("🐶", "dog", "pet"),
        entry("🐱", "cat", "pet"),
        entry("🐾", "paw", "pet", "vet"),
        entry("🐟", "fish", "aquarium", "pet"),
        entry("🐦", "bird", "pet"),
        entry("🌸", "flowers", "gift"),

        // Money
        entry("💰", "money", "savings", "bag"),
        entry("💵", "cash", "notes", "money"),
        entry("🏦", "bank", "money"),
        entry("🏧", "atm", "withdrawal", "cash"),
        entry("📈", "investment", "stocks", "profit"),
        entry("📉", "loss", "market", "stocks"),
        entry("💸", "spending", "money"),
        entry("🧮", "accounting", "calculation"),
        entry("🤝", "loan", "deal", "agreement"),
        entry("📊", "budget", "chart", "report"),
        entry("🗓️", "calendar", "subscription", "monthly"),
        entry("⏰", "alarm", "reminder", "due"),
        entry("🛡️", "insurance", "protection", "premium"),

        // Work and deliveries
        entry("💼", "work", "business", "briefcase"),
        entry("🏭", "factory", "industry"),
        entry("🚚", "delivery", "truck", "courier"),
        entry("📦", "package", "parcel", "delivery"),
        entry("📮", "post", "mail", "courier"),
        entry("✉️", "mail", "letter", "post"),
        entry("🖇️", "office supplies", "clip"),
        entry("🗄️", "files", "cabinet", "office"),

        // Giving and everything else
        entry("🕌", "mosque", "religious", "donation"),
        entry("⛪", "church", "religious", "donation"),
        entry("🙏", "prayer", "temple", "donation", "thanks"),
        entry("🆘", "emergency", "help", "urgent"),
        entry("⭐", "favourite", "favorite", "star"),
        entry("🔖", "bookmark", "tag"),
        entry("❓", "unknown", "other", "misc"),
        entry("📌", "pin", "other", "misc"),
    )

    /** Every emoji in catalogue order, for the grid with no search term. */
    val emojis: List<String> = entries.map { it.emoji }

    /**
     * Entries matching [query], ranked: a keyword the query matches outright,
     * then one it starts, then one it merely appears inside. Without the first
     * rank, "car" leads with the shopping cart, because "cart" starts with it
     * too. The sort is stable, so catalogue order survives inside each rank.
     *
     * A blank query is not a filter; it returns everything.
     */
    fun search(query: String): List<Entry> {
        val needle = query.trim().lowercase()
        if (needle.isEmpty()) return entries

        return entries
            .filter { candidate -> candidate.keywords.any { it.contains(needle) } }
            .sortedBy { candidate ->
                when {
                    candidate.keywords.any { it == needle } -> 0
                    candidate.keywords.any { it.startsWith(needle) } -> 1
                    else -> 2
                }
            }
    }
}
