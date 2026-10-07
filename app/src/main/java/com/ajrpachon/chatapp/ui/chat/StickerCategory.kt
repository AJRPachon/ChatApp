package com.ajrpachon.chatapp.ui.chat

internal data class StickerCategory(val name: String, val emojis: List<String>)

internal val stickerPacks = listOf(
    StickerCategory("😀 Caras", listOf(
        "😀","😂","🥰","😍","🤩","😎","🥺","😭","😱","🤔",
        "😏","🙄","😤","🥳","🤗","😅","🤣","😇","🥲","😬",
    )),
    StickerCategory("❤️ Amor", listOf(
        "❤️","💕","💖","💗","💓","💞","💝","💘","💟","🩷",
        "🧡","💛","💚","💙","💜","🖤","🤍","🤎","💔","❣️",
    )),
    StickerCategory("🐶 Animales", listOf(
        "🐶","🐱","🐻","🦊","🐼","🦁","🐯","🐨","🐸","🐙",
        "🦋","🐬","🦄","🐝","🦀","🐠","🐧","🦆","🐺","🦝",
    )),
    StickerCategory("🍕 Comida", listOf(
        "🍕","🍔","🍣","🎂","🍩","☕","🍦","🍰","🍜","🍿",
        "🥑","🍓","🍉","🥐","🧁","🌮","🍟","🍙","🥞","🍫",
    )),
    StickerCategory("⚽ Actividades", listOf(
        "⚽","🎮","🎵","🎸","🎤","🏆","🎯","🎨","🎬","📚",
        "💻","📱","🎳","🎲","♟️","🏋️","🧩","🎭","🎪","🚀",
    )),
    StickerCategory("🌸 Naturaleza", listOf(
        "🌸","🌺","🌻","🌊","🌈","⭐","🌙","☀️","❄️","🌿",
        "🍀","🌴","🌵","🍁","🌷","🦋","🌟","💫","🔥","🌍",
    )),
    StickerCategory("👍 Gestos", listOf(
        "👍","👎","🙌","👏","🤝","✌️","🤞","👋","🤙","💪",
        "🫶","🤜","🫵","👆","🙏","✊","🤘","👌","🫠","🤷",
    )),
    StickerCategory("🎉 Fiesta", listOf(
        "🎉","🎊","🎈","🥂","🍾","✨","💯","🔥","🏅","🎁",
        "🎆","🎇","🪅","🎀","🛍️","🪄","🌠","🎠","🎡","🎢",
    )),
)
