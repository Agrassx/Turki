package com.turki.admin.web.ui

import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.JustifyContent
import org.jetbrains.compose.web.css.LineStyle
import org.jetbrains.compose.web.css.Position
import org.jetbrains.compose.web.css.StyleSheet
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.background
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.css.border
import org.jetbrains.compose.web.css.borderRadius
import org.jetbrains.compose.web.css.boxSizing
import org.jetbrains.compose.web.css.color
import org.jetbrains.compose.web.css.cursor
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flex
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.fontFamily
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.css.fontWeight
import org.jetbrains.compose.web.css.gap
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.justifyContent
import org.jetbrains.compose.web.css.letterSpacing
import org.jetbrains.compose.web.css.lineHeight
import org.jetbrains.compose.web.css.margin
import org.jetbrains.compose.web.css.marginBottom
import org.jetbrains.compose.web.css.marginTop
import org.jetbrains.compose.web.css.minHeight
import org.jetbrains.compose.web.css.opacity
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.paddingBottom
import org.jetbrains.compose.web.css.paddingTop
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.position
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.textAlign
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.css.media

object AppStyles : StyleSheet() {
    val fadeUp by keyframes {
        from {
            opacity(0)
            property("transform", "translateY(12px)")
        }
        to {
            opacity(1)
            property("transform", "translateY(0)")
        }
    }

    val spin by keyframes {
        from { property("transform", "rotate(0deg)") }
        to { property("transform", "rotate(360deg)") }
    }

    init {
        ":root" style {
            property("--color-bg", "#0B101A")
            property("--color-surface", "#141B2D")
            property("--color-surface-2", "#182135")
            property("--color-card", "#151E32")
            property("--color-border", "#24324A")
            property("--color-text", "#E7EDF4")
            property("--color-muted", "#97A3B5")
            property("--color-accent", "#4BC6B9")
            property("--color-accent-2", "#3BA4E7")
            property("--color-danger", "#E76F51")
            property("--color-warning", "#F4A261")
            property("--color-success", "#2A9D8F")
            property("--radius-sm", "10px")
            property("--radius-md", "16px")
            property("--radius-lg", "22px")
            property("--shadow-1", "0 12px 30px rgba(5, 10, 25, 0.35)")
            property("--shadow-2", "0 20px 50px rgba(5, 10, 25, 0.45)")
        }

        "html, body" style {
            width(100.percent)
            height(100.percent)
            margin(0.px)
        }

        "body" style {
            fontFamily("Space Grotesk", "IBM Plex Sans", "sans-serif")
            background(
                "radial-gradient(1200px 800px at 10% 0%, rgba(59, 164, 231, 0.15), transparent 55%), " +
                    "radial-gradient(1000px 700px at 90% 10%, rgba(75, 198, 185, 0.18), transparent 60%), " +
                    "linear-gradient(180deg, #0B101A 0%, #0D1424 100%)"
            )
            color(Color("var(--color-text)"))
            property("overflow", "hidden")
        }

        "#root" style {
            height(100.percent)
        }

        "*" style {
            boxSizing("border-box")
        }

        "button" style {
            fontFamily("Space Grotesk", "IBM Plex Sans", "sans-serif")
        }
    }

    val appRoot by style {
        width(100.percent)
        height(100.percent)
        display(DisplayStyle.Flex)
        padding(24.px)
    }

    val appShell by style {
        width(100.percent)
        height(100.percent)
        display(DisplayStyle.Flex)
        gap(24.px)
    }

    val sidebar by style {
        width(260.px)
        height(100.percent)
        display(DisplayStyle.Flex)
        flexDirection(FlexDirection.Column)
        padding(20.px)
        borderRadius(22.px)
        border(1.px, LineStyle.Solid, Color("var(--color-border)"))
        property("background", "linear-gradient(160deg, #151E32 0%, #111827 100%)")
        property("box-shadow", "var(--shadow-1)")
        property("backdrop-filter", "blur(6px)")
    }

    val sidebarBrand by style {
        display(DisplayStyle.Flex)
        flexDirection(FlexDirection.Column)
        gap(6.px)
        marginBottom(24.px)
    }

    val sidebarTitle by style {
        fontSize(22.px)
        fontWeight(700)
        color(Color("var(--color-text)"))
    }

    val sidebarSubtitle by style {
        fontSize(12.px)
        fontWeight(500)
        letterSpacing(1.px)
        property("text-transform", "uppercase")
        color(Color("var(--color-muted)"))
    }

    val navItem by style {
        display(DisplayStyle.Flex)
        alignItems(AlignItems.Center)
        gap(10.px)
        padding(12.px, 14.px)
        borderRadius(14.px)
        color(Color("var(--color-text)"))
        fontSize(15.px)
        fontWeight(600)
        cursor("pointer")
        property("transition", "all 180ms ease")
    }

    val navItemActive by style {
        property("background", "linear-gradient(135deg, rgba(59, 164, 231, 0.22), rgba(75, 198, 185, 0.22))")
        color(Color("var(--color-text)"))
        property("box-shadow", "inset 0 0 0 1px rgba(75, 198, 185, 0.35)")
    }

    val navSpacer by style {
        flex(1)
    }

    val sidebarFooter by style {
        color(Color("var(--color-muted)"))
        fontSize(12.px)
    }

    val mainContent by style {
        flex(1)
        height(100.percent)
        borderRadius(24.px)
        padding(28.px)
        border(1.px, LineStyle.Solid, Color("var(--color-border)"))
        property(
            "background",
            "linear-gradient(180deg, rgba(20, 27, 45, 0.9) 0%, rgba(15, 21, 36, 0.95) 100%)"
        )
        property("box-shadow", "var(--shadow-2)")
        property("overflow", "auto")
    }

    val page by style {
        display(DisplayStyle.Flex)
        flexDirection(FlexDirection.Column)
        gap(20.px)
        minHeight(100.percent)
        property("animation", "${fadeUp.name} 320ms ease-out")
        property("animation-fill-mode", "both")
    }

    val pageHeader by style {
        display(DisplayStyle.Flex)
        justifyContent(JustifyContent.SpaceBetween)
        alignItems(AlignItems.Center)
        gap(16.px)
        property("flex-wrap", "wrap")
    }

    val pageTitle by style {
        fontSize(30.px)
        fontWeight(700)
        margin(0.px)
    }

    val pageSubtitle by style {
        color(Color("var(--color-muted)"))
        fontSize(13.px)
    }

    val actionsRow by style {
        display(DisplayStyle.Flex)
        gap(12.px)
        property("flex-wrap", "wrap")
    }

    val statsGrid by style {
        display(DisplayStyle.Grid)
        gap(16.px)
        property("grid-template-columns", "repeat(auto-fit, minmax(200px, 1fr))")
    }

    val statCard by style {
        padding(16.px)
        borderRadius(18.px)
        border(1.px, LineStyle.Solid, Color("var(--color-border)"))
        property("background", "rgba(18, 26, 42, 0.9)")
        property("box-shadow", "0 12px 26px rgba(6, 12, 24, 0.25)")
        property("border-left", "3px solid var(--stat-accent)")
        property("animation", "${fadeUp.name} 420ms ease-out")
        property("animation-fill-mode", "both")
        property("animation-delay", "var(--stagger-delay, 0ms)")
    }

    val statLabel by style {
        fontSize(12.px)
        property("text-transform", "uppercase")
        letterSpacing(1.px)
        color(Color("var(--color-muted)"))
        marginBottom(8.px)
        display(DisplayStyle.Block)
    }

    val statValue by style {
        fontSize(26.px)
        fontWeight(700)
        color(Color("var(--color-text)"))
    }

    val statHint by style {
        fontSize(12.px)
        color(Color("var(--color-muted)"))
        marginTop(6.px)
        display(DisplayStyle.Block)
    }

    val cardList by style {
        display(DisplayStyle.Flex)
        flexDirection(FlexDirection.Column)
        gap(14.px)
    }

    val card by style {
        padding(18.px)
        borderRadius(18.px)
        border(1.px, LineStyle.Solid, Color("var(--color-border)"))
        property("background", "rgba(21, 30, 50, 0.94)")
        property("box-shadow", "0 10px 24px rgba(4, 8, 18, 0.28)")
        property("animation", "${fadeUp.name} 460ms ease-out")
        property("animation-fill-mode", "both")
        property("animation-delay", "var(--stagger-delay, 0ms)")
    }

    val cardSplit by style {
        display(DisplayStyle.Grid)
        gap(16.px)
        property("grid-template-columns", "minmax(0, 1fr) auto")
        property("align-items", "center")
    }

    val cardMain by style {
        display(DisplayStyle.Flex)
        flexDirection(FlexDirection.Column)
        gap(6.px)
    }

    val cardHeader by style {
        display(DisplayStyle.Flex)
        justifyContent(JustifyContent.SpaceBetween)
        alignItems(AlignItems.Center)
        gap(16.px)
    }

    val headerGroup by style {
        display(DisplayStyle.Flex)
        alignItems(AlignItems.Center)
        gap(12.px)
    }

    val titleStack by style {
        display(DisplayStyle.Flex)
        flexDirection(FlexDirection.Column)
        gap(4.px)
    }

    val cardTitle by style {
        fontSize(16.px)
        fontWeight(600)
        color(Color("var(--color-text)"))
    }

    val cardMeta by style {
        fontSize(13.px)
        color(Color("var(--color-muted)"))
    }

    val metaRow by style {
        display(DisplayStyle.Flex)
        gap(16.px)
        property("flex-wrap", "wrap")
    }

    val cardActions by style {
        display(DisplayStyle.Flex)
        alignItems(AlignItems.Center)
        gap(12.px)
    }

    val columnActions by style {
        display(DisplayStyle.Flex)
        flexDirection(FlexDirection.Column)
        alignItems(AlignItems.FlexEnd)
        gap(10.px)
    }

    val button by style {
        display(DisplayStyle.LegacyInlineFlex)
        alignItems(AlignItems.Center)
        gap(8.px)
        padding(10.px, 18.px)
        borderRadius(12.px)
        border(0.px)
        fontSize(14.px)
        fontWeight(600)
        cursor("pointer")
        property("transition", "transform 160ms ease, box-shadow 160ms ease, background 160ms ease")
    }

    val buttonPrimary by style {
        property("background", "linear-gradient(135deg, #3BA4E7 0%, #4BC6B9 100%)")
        color(Color("#0B111C"))
        property("box-shadow", "0 10px 20px rgba(59, 164, 231, 0.3)")
    }

    val buttonDanger by style {
        backgroundColor(Color("var(--color-danger)"))
        color(Color("#140A07"))
    }

    val buttonGhost by style {
        backgroundColor(Color("transparent"))
        border(1.px, LineStyle.Solid, Color("var(--color-border)"))
        color(Color("var(--color-text)"))
    }

    val buttonSoft by style {
        backgroundColor(Color("rgba(75, 198, 185, 0.15)"))
        color(Color("var(--color-text)"))
        border(1.px, LineStyle.Solid, Color("rgba(75, 198, 185, 0.35)"))
    }

    val pill by style {
        display(DisplayStyle.LegacyInlineFlex)
        alignItems(AlignItems.Center)
        gap(6.px)
        padding(4.px, 10.px)
        borderRadius(999.px)
        fontSize(12.px)
        fontWeight(600)
    }

    val pillActive by style {
        backgroundColor(Color("rgba(42, 157, 143, 0.2)"))
        color(Color("var(--color-success)"))
        border(1.px, LineStyle.Solid, Color("rgba(42, 157, 143, 0.35)"))
    }

    val pillInactive by style {
        backgroundColor(Color("rgba(148, 163, 184, 0.14)"))
        color(Color("var(--color-muted)"))
        border(1.px, LineStyle.Solid, Color("rgba(148, 163, 184, 0.25)"))
    }

    val iconBadge by style {
        display(DisplayStyle.LegacyInlineFlex)
        alignItems(AlignItems.Center)
        justifyContent(JustifyContent.Center)
        width(36.px)
        height(36.px)
        borderRadius(12.px)
        backgroundColor(Color("rgba(59, 164, 231, 0.18)"))
        color(Color("#A7D8F5"))
        fontSize(18.px)
    }

    val loadingState by style {
        display(DisplayStyle.Flex)
        flexDirection(FlexDirection.Column)
        alignItems(AlignItems.Center)
        justifyContent(JustifyContent.Center)
        gap(12.px)
        padding(24.px)
        color(Color("var(--color-muted)"))
    }

    val switchWrap by style {
        display(DisplayStyle.LegacyInlineFlex)
        alignItems(AlignItems.Center)
        gap(8.px)
    }

    val switchLabel by style {
        fontSize(12.px)
        color(Color("var(--color-muted)"))
    }

    val switchContainer by style {
        position(Position.Relative)
        width(46.px)
        height(26.px)
    }

    val switchInput by style {
        opacity(0)
        width(46.px)
        height(26.px)
        margin(0.px)
        position(Position.Absolute)
        property("top", "0")
        property("left", "0")
        cursor("pointer")
        property("z-index", 2)
    }

    val switchTrack by style {
        position(Position.Absolute)
        property("top", "0")
        property("left", "0")
        property("right", "0")
        property("bottom", "0")
        borderRadius(999.px)
        backgroundColor(Color("rgba(148, 163, 184, 0.22)"))
        border(1.px, LineStyle.Solid, Color("rgba(148, 163, 184, 0.35)"))
        property("transition", "all 160ms ease")
    }

    val switchThumb by style {
        position(Position.Absolute)
        property("top", "3px")
        property("left", "3px")
        width(18.px)
        height(18.px)
        borderRadius(999.px)
        backgroundColor(Color("#F8FAFC"))
        property("box-shadow", "0 4px 10px rgba(0,0,0,0.3)")
        property("transition", "transform 160ms ease")
    }

    val spinner by style {
        width(40.px)
        height(40.px)
        borderRadius(50.percent)
        border(3.px, LineStyle.Solid, Color("rgba(148, 163, 184, 0.25)"))
        property("border-top", "3px solid var(--color-accent-2)")
        property("animation", "${spin.name} 1.1s linear infinite")
    }

    val divider by style {
        width(100.percent)
        height(1.px)
        backgroundColor(Color("rgba(148, 163, 184, 0.2)"))
    }

    val microLabel by style {
        fontSize(11.px)
        property("text-transform", "uppercase")
        letterSpacing(1.px)
        color(Color("var(--color-muted)"))
    }

    val noteText by style {
        fontSize(12.px)
        color(Color("var(--color-muted)"))
    }

    val chip by style {
        display(DisplayStyle.LegacyInlineFlex)
        alignItems(AlignItems.Center)
        gap(6.px)
        padding(4.px, 8.px)
        borderRadius(999.px)
        fontSize(12.px)
        color(Color("var(--color-text)"))
        backgroundColor(Color("rgba(59, 164, 231, 0.2)"))
        border(1.px, LineStyle.Solid, Color("rgba(59, 164, 231, 0.3)"))
    }

    val paragraph by style {
        lineHeight("1.5")
    }

    init {
        style(selector(".${navItem}:hover")) {
            backgroundColor(Color("rgba(255, 255, 255, 0.06)"))
        }

        style(selector(".${button}:hover")) {
            property("transform", "translateY(-1px)")
            property("box-shadow", "0 12px 26px rgba(4, 8, 18, 0.3)")
        }

        style(selector(".${switchInput}:checked + .${switchTrack}")) {
            backgroundColor(Color("rgba(75, 198, 185, 0.35)"))
            border(1.px, LineStyle.Solid, Color("rgba(75, 198, 185, 0.5)"))
        }

        style(selector(".${switchInput}:checked + .${switchTrack} .${switchThumb}")) {
            property("transform", "translateX(20px)")
        }

        media("screen and (max-width: 980px)") {
            style(selector(".${appRoot}")) {
                padding(16.px)
            }
            style(selector(".${appShell}")) {
                flexDirection(FlexDirection.Column)
            }
            style(selector(".${sidebar}")) {
                width(100.percent)
                property("height", "auto")
            }
            style(selector(".${mainContent}")) {
                padding(20.px)
            }
            style(selector(".${cardSplit}")) {
                property("grid-template-columns", "1fr")
            }
            style(selector(".${columnActions}")) {
                alignItems(AlignItems.FlexStart)
            }
        }
    }
}
