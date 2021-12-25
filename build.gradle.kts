import myaa.subkt.ass.*
import myaa.subkt.tasks.*
import myaa.subkt.tasks.Mux.*
import myaa.subkt.tasks.Nyaa.*
import java.awt.Color
import java.time.*

plugins {
    id("myaa.subkt")
}

fun String.isKaraTemplate(): Boolean {
    return this.startsWith("code") || this.startsWith("template") || this.startsWith("mixin")
}

fun EventLine.isKaraTemplate(): Boolean {
    return this.comment && this.effect.isKaraTemplate()
}

subs {
    readProperties("sub.properties")
    episodes(getList("episodes"))

    val op_ktemplate by task<Automation> {
        if (propertyExists("OP")) {
            from(get("OP"))
        }

        video(get("premux"))
        script("0x.KaraTemplater.moon")
        macro("0x539's Templater")
        loglevel(Automation.LogLevel.WARNING)
    }

    val op_title by task<ASS> {
        from(op_ktemplate.item())

        val ep_title = getRaw("eptitle")
        val ep_subtitle = getRaw("subtitle")

        ass {
                for (line in events.lines) {
                    line.text = line.text
                        .replace("EP_TITLE", ep_title)
                        .replace("EP_SUBTITLE", ep_subtitle)
                }
            }
        }


    val ed_ktemplate by task<Automation> {
        if (propertyExists("ED")) {
            from(get("ED"))
        }

        video(get("premux"))
        script("0x.KaraTemplater.moon")
        macro("0x539's Templater")
        loglevel(Automation.LogLevel.WARNING)
    }

    merge {
        from(get("dialogue"))

        if (propertyExists("OP")) {
            from(op_ktemplate.item()) {
                syncSourceLine("sync")
                syncTargetLine("opsync")
            }
        }

        if (propertyExists("ED")) {
            from(ed_ktemplate.item()) {
                syncSourceLine("sync")
                syncTargetLine("edsync")
            }
        }

        fromIfPresent(get("INS"), ignoreMissingFiles = true)
        fromIfPresent(getList("TS"), ignoreMissingFiles = true)

        includeExtraData(false)
        includeProjectGarbage(false)

        scriptInfo {
            title = "Kaleido-subs"
            scaledBorderAndShadow = true
        }
    }

    val cleanmerge by task<ASS> {
        from(merge.item())
        ass {
            events.lines.removeIf { it.isKaraTemplate() }
        }
    }

    chapters {
        from(cleanmerge.item())
        chapterMarker("chapter")
    }

    swap { from(cleanmerge.item()) }

    mux {
        title(get("title"))

        skipUnusedFonts(true)

        from(get("premux")) {
            video {
                lang("jpn")
                default(true)
            }
            audio() {
                lang("jpn")
                default(true)
            }
            includeChapters(false)
            attachments { include(false) }
        }

        from(cleanmerge.item()) {
            subtitles {
                lang("eng")
                name(get("group_reg"))
                default(true)
                forced(false)
                compression(CompressionType.ZLIB)
            }
        }

        from(swap.item()) {
            subtitles {
                lang("enm")
                name(get("group_hono"))
                default(false)
                forced(false)
                compression(CompressionType.ZLIB)
            }
        }

        chapters(chapters.item()) { lang("eng") }

        attach(get("common_fonts")) {
            includeExtensions("ttf", "otf", "ttc")
        }

        attach(get("fonts")) {
            includeExtensions("ttf", "otf", "ttc")
        }

        if (propertyExists("OP")) {
            attach(get("opfonts")) {
                includeExtensions("ttf", "otf", "ttc")
            }
        }

        if (propertyExists("ED")) {
            attach(get("edfonts")) {
                includeExtensions("ttf", "otf", "ttc")
            }
        }

        out(get("muxout"))
    }
}
