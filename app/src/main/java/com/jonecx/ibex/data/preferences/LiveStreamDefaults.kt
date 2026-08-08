package com.jonecx.ibex.data.preferences

import com.jonecx.azmaree.player.model.StreamingProtocol.DASH
import com.jonecx.azmaree.player.model.StreamingProtocol.HLS
import com.jonecx.ibex.data.model.VideoFeed

/**
 * The Azmaree app's built-in stream list, in order, seeded into the Live store on first launch so the
 * grid isn't empty out of the box. Stable ids match Azmaree's so re-seeding never duplicates, and the
 * seed runs once (see [LiveStreamsPreferences.seedIfNeeded]) — deleting a stream keeps it deleted.
 */
object LiveStreamDefaults {

    val AZMAREE_STREAMS: List<VideoFeed> = listOf(
        VideoFeed(
            id = "0, BBC World Service",
            title = "BBC World Service",
            url = "https://a.files.bbci.co.uk/ms6/live/3441A116-B12E-4D2F-ACA8-C1984642FA4B/audio/simulcast/hls/nonuk/iptv_hd_abr_v1/aks/bbc_world_service_news_internet.m3u8",
            thumbnailUrl = "https://rts.org.uk/sites/default/files/styles/12_column/public/p02wkrw1_002.jpg",
            protocol = HLS,
            description = "The BBC World Service brings you news, analysis and stories from around the world, broadcasting 24/7 in English.",
        ),
        VideoFeed(
            id = "1, BBC News",
            title = "BBC News (HLS live)",
            url = "https://vs-hls-push-ww-live.akamaized.net/x=4/i=urn:bbc:pips:service:bbc_news_channel_hd/mobile_wifi_main_hd_abr_v2.m3u8",
            thumbnailUrl = "https://ichef.bbci.co.uk/images/ic/1008x567/p0g6j1tq.jpg",
            protocol = HLS,
        ),
        VideoFeed(
            id = "2, Mux",
            title = "Mux test stream (HLS)",
            url = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
            thumbnailUrl = "https://resizing.flixster.com/oTpDKu9NB_3Uu5QCr7XTiB-Y8gw=/375x210/v2/https://resizing.flixster.com/-XZAfHZM39UwaGJIFWKAE8fS0ak=/v3/t/assets/p10449498_i_h10_aa.jpg",
            protocol = HLS,
        ),
        VideoFeed(
            id = "3, BBC Arabic",
            title = "BBC News Arabic (HLS live)",
            url = "https://vs-hls-pushb-ww-live.akamaized.net/x=4/i=urn:bbc:pips:service:bbc_arabic_tv/pc_hd_abr_v2.m3u8",
            thumbnailUrl = "https://ichef.bbci.co.uk/ace/ws/304/amz/worldservice/live/assets/images/2011/01/18/110118143226_arabic_promo_304x171.jpg.webp",
            protocol = HLS,
        ),
        VideoFeed(
            id = "4, BBC Persian",
            title = "BBC News Persian (HLS live)",
            url = "https://vs-hls-pushb-ww-live.akamaized.net/x=4/i=urn:bbc:pips:service:bbc_persian_tv/mobile_wifi_main_hd_abr_v2.m3u8",
            thumbnailUrl = "https://i.ytimg.com/vi/62SWtSabNS8/maxresdefault.jpg",
            protocol = HLS,
        ),
        VideoFeed(
            id = "5, Euronews",
            title = "Euronews English (HLS live)",
            url = "https://streams.sofast.tv/euronewsen/live/eds/euronews-en/25017/euronews-en.m3u8",
            thumbnailUrl = "https://picsum.photos/id/1001/534/356",
            protocol = HLS,
        ),
        VideoFeed(
            id = "6, Moji TV",
            title = "Moji TV (HLS live)",
            url = "https://odmedia-mojitv-1-be.samsung.wurl.tv/playlist.m3u8",
            thumbnailUrl = "https://picsum.photos/id/1025/534/356",
            protocol = HLS,
        ),
        VideoFeed(
            id = "7, Just For Laughs",
            title = "Just For Laughs Gags (HLS live)",
            url = "https://distributionsjustepourrire-justforlaughsgags-1-be.samsung.wurl.tv/playlist.m3u8",
            thumbnailUrl = "https://picsum.photos/id/1039/534/356",
            protocol = HLS,
        ),
        VideoFeed(
            id = "8, Go USA",
            title = "Go USA (HLS live)",
            url = "https://brandusa-gousa-1-be.samsung.wurl.tv/playlist.m3u8",
            thumbnailUrl = "https://picsum.photos/id/1084/534/356",
            protocol = HLS,
        ),
        VideoFeed(
            id = "9, BipBop",
            title = "Apple BipBop (HLS, subtitles)",
            url = "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_16x9/bipbop_16x9_variant.m3u8",
            thumbnailUrl = "https://picsum.photos/id/1015/534/356",
            protocol = HLS,
        ),
        VideoFeed(
            id = "10, Angel One",
            title = "Angel One (DASH, subtitles)",
            url = "https://storage.googleapis.com/shaka-demo-assets/angel-one/dash.mpd",
            thumbnailUrl = "https://picsum.photos/id/1043/534/356",
            protocol = DASH,
        ),
        VideoFeed(
            id = "11, Al Jazeera English",
            title = "Al Jazeera English (HLS live)",
            url = "https://live-hls-apps-aje-fa.getaj.net/AJE/index.m3u8",
            thumbnailUrl = "https://i.imgur.com/7bRVpnu.png",
            protocol = HLS,
        ),
        VideoFeed(
            id = "12, Sky News",
            title = "Sky News (HLS live)",
            url = "https://jmp2.uk/plu-55b285cd2665de274553d66f.m3u8",
            thumbnailUrl = "https://d2n0069hmnqmmx.cloudfront.net/epgdata/1.0/newchanlogos/512/512/skychb1404.png",
            protocol = HLS,
        ),
    )
}
