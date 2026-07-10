package com.bainian.memory.ui;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.bainian.memory.data.RecordRepository;
import com.bainian.memory.model.MemoryRecord;
import com.bainian.memory.util.UiKit;
import com.bainian.memory.util.WidgetFactory;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * 「百年」——不是一个查数据的工具页，而是一次「安静地回望人生」的主动探访。
 *
 * 设计立意：把人生具象成 100 个格子，本身就是一件很有冲击力的事情——
 * 用户点进来，不是想「查某一年」（日历已经能做这件事），
 * 而是想获得一种「审视自己人生」的仪式感：我已经走过多少，还剩多少，
 * 这些留白的格子，我打算怎么去填。
 *
 * 因此整页刻意弱化「表格 / 数据面板」的工具感：
 *   - 头部不是干巴巴的「生命进度」标题，而是一句随年龄变化的感性文案；
 *   - 核心对比是「已走过」与「尚可期待」两个大数字，而不是并列的统计小字；
 *   - 年份网格降级为背景质感的圆点矩阵，去掉数字与边框噪音，点击才展开细节。
 *
 * 作为次级页面使用（从「我的」页顶部卡片进入），带返回箭头。
 */
public class CenturyPage {
    private final Context ctx;
    private final RecordRepository repo;
    private final PageHost host;
    private final Runnable onBack;

    public CenturyPage(Context ctx, RecordRepository repo, PageHost host) {
        this(ctx, repo, host, null);
    }

    public CenturyPage(Context ctx, RecordRepository repo, PageHost host, Runnable onBack) {
        this.ctx = ctx;
        this.repo = repo;
        this.host = host;
        this.onBack = onBack;
    }

    public View build() {
        ScrollView scroll = new ScrollView(ctx);
        scroll.setClipToPadding(false);
        LinearLayout body = WidgetFactory.pageBody(ctx);
        body.setPadding(UiKit.dp(ctx, 24), UiKit.dp(ctx, 12), UiKit.dp(ctx, 24), UiKit.dp(ctx, 40));
        scroll.addView(body);

        if (onBack != null) {
            TextView back = new TextView(ctx);
            back.setText("‹ 返回");
            back.setTextSize(16);
            back.setTextColor(UiKit.getColorByName(ctx, "ink"));
            back.setPadding(0, 0, 0, UiKit.dp(ctx, 20));
            back.setOnClickListener(v -> onBack.run());
            body.addView(back);
        }

        int birthYear = repo.getBirthYear();
        int currentYear = LocalDate.now().getYear();
        int age = Math.min(Math.max(0, currentYear - birthYear), 100);
        int remaining = 100 - age;
        int livedDays = repo.countRecordedDays();
        int recordedYears = 0;
        for (int i = 0; i < 100; i++) {
            if (repo.recordCountForYear(birthYear + i) > 0) recordedYears++;
        }

        body.addView(buildHeroSection(age, remaining, recordedYears, livedDays));
        body.addView(buildBoardCard(birthYear, currentYear));
        body.addView(buildFooterNote());

        return scroll;
    }

    // ── 头部：感性文案 + 已走过/尚可期待 的大数字对比 ──
    private View buildHeroSection(int age, int remaining, int recordedYears, int livedDays) {
        LinearLayout section = new LinearLayout(ctx);
        section.setOrientation(LinearLayout.VERTICAL);
        section.setLayoutParams(WidgetFactory.fullWidthMargin(ctx, 0, UiKit.dp(ctx, 28)));

        TextView poetic = new TextView(ctx);
        poetic.setText(poeticLine(age, remaining));
        poetic.setTextColor(UiKit.getColorByName(ctx, "muted"));
        poetic.setTextSize(14);
        poetic.setLineSpacing(UiKit.dp(ctx, 4), 1f);
        poetic.setPadding(0, 0, 0, UiKit.dp(ctx, 22));
        section.addView(poetic);

        // 已走过 / 尚可期待 —— 两个巨大数字并排，视觉重心所在
        LinearLayout numbersRow = new LinearLayout(ctx);
        numbersRow.setOrientation(LinearLayout.HORIZONTAL);
        numbersRow.addView(bigNumberBlock(String.valueOf(age), "年已走过",
                UiKit.getColorByName(ctx, "ink")));

        View divider = new View(ctx);
        divider.setBackgroundColor(UiKit.getColorByName(ctx, "muted_light"));
        LinearLayout.LayoutParams dividerLp = new LinearLayout.LayoutParams(UiKit.dp(ctx, 1), UiKit.dp(ctx, 44));
        dividerLp.gravity = Gravity.CENTER_VERTICAL;
        dividerLp.setMargins(UiKit.dp(ctx, 18), 0, UiKit.dp(ctx, 18), 0);
        numbersRow.addView(divider, dividerLp);

        numbersRow.addView(bigNumberBlock(remaining > 0 ? String.valueOf(remaining) : "0", "年尚可期待",
                UiKit.getColorByName(ctx, "accent")));
        section.addView(numbersRow);

        // 细的年龄进度线 —— 保留一点点「刻度感」但极简，不做成进度条控件式样
        LinearLayout track = new LinearLayout(ctx);
        track.setOrientation(LinearLayout.HORIZONTAL);
        track.setBackground(UiKit.getDrawableByName(ctx, "progress_track"));
        track.setClipToOutline(true);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, UiKit.dp(ctx, 4));
        tp.setMargins(0, UiKit.dp(ctx, 20), 0, UiKit.dp(ctx, 10));
        track.setLayoutParams(tp);
        View fill = new View(ctx);
        fill.setBackgroundColor(UiKit.getColorByName(ctx, "ink"));
        int fw = Math.max(age, 1);
        track.addView(fill, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, fw));
        if (age < 100) {
            track.addView(new View(ctx), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 100 - fw));
        }
        section.addView(track);

        TextView stat = new TextView(ctx);
        stat.setText("这一路，你留下了 " + recordedYears + " 个被记住的年份 · " + livedDays + " 个被点亮的日子");
        stat.setTextColor(UiKit.getColorByName(ctx, "muted"));
        stat.setTextSize(12);
        section.addView(stat);

        return section;
    }

    private View bigNumberBlock(String number, String label, int color) {
        LinearLayout block = new LinearLayout(ctx);
        block.setOrientation(LinearLayout.VERTICAL);

        TextView num = new TextView(ctx);
        num.setText(number);
        num.setTextColor(color);
        num.setTextSize(44);
        num.setTypeface(Typeface.DEFAULT_BOLD);
        num.setIncludeFontPadding(false);
        block.addView(num);

        TextView lb = new TextView(ctx);
        lb.setText(label);
        lb.setTextColor(UiKit.getColorByName(ctx, "muted"));
        lb.setTextSize(12);
        lb.setPadding(0, UiKit.dp(ctx, 2), 0, 0);
        block.addView(lb);

        return block;
    }

    /** 根据年龄生成一句感性文案，弱化数据感，强化「时间流逝」的氛围 */
    private String poeticLine(int age, int remaining) {
        if (remaining <= 0) {
            return "百年已至。但故事，从来不止于格子的多少。";
        }
        if (age < 18) {
            return "人生的画卷才刚刚展开，往后的每一页，\n都由你此刻的选择书写。";
        }
        if (age < 40) {
            return "你已经认真地活了 " + age + " 年。\n往后还有很长的路，值得慢慢走、好好记。";
        }
        if (age < 65) {
            return "岁月过半，但精彩往往厚积在后半程。\n剩下的时间，比想象中更值得珍惜。";
        }
        return "人生走到了从容的阶段。\n每一天的点亮，都是留给未来的礼物。";
    }

    // ── 年份网格：弱化为柔和圆点矩阵，去掉数字与方框边界 ──
    private View buildBoardCard(int birthYear, int currentYear) {
        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(UiKit.dp(ctx, 16), UiKit.dp(ctx, 20), UiKit.dp(ctx, 16), UiKit.dp(ctx, 16));
        card.setBackground(UiKit.getDrawableByName(ctx, "card_elevated"));
        card.setLayoutParams(WidgetFactory.fullWidthMargin(ctx, 0, UiKit.dp(ctx, 18)));

        TextView title = new TextView(ctx);
        title.setText("这一生的样子");
        title.setTextColor(UiKit.getColorByName(ctx, "ink"));
        title.setTextSize(15);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(UiKit.dp(ctx, 4), 0, 0, UiKit.dp(ctx, 14));
        card.addView(title);

        for (int dec = 0; dec < 10; dec++) {
            int decStart = birthYear + dec * 10;
            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, UiKit.dp(ctx, 3), 0, UiKit.dp(ctx, 3));
            for (int j = 0; j < 10; j++) {
                row.addView(yearDot(decStart + j, currentYear));
            }
            card.addView(row);
        }

        // 极简图例——只用小圆点+文字，不做方框
        LinearLayout legend = new LinearLayout(ctx);
        legend.setOrientation(LinearLayout.HORIZONTAL);
        legend.setGravity(Gravity.CENTER);
        legend.setPadding(0, UiKit.dp(ctx, 14), 0, 0);
        legend.addView(legendDot(UiKit.getColorByName(ctx, "accent"), "有记录"));
        legend.addView(legendDot(UiKit.getColorByName(ctx, "ink"), "今年"));
        legend.addView(legendDot(UiKit.getColorByName(ctx, "muted_light"), "空白"));
        card.addView(legend);

        return card;
    }

    private View yearDot(int year, int currentYear) {
        int count = repo.recordCountForYear(year);
        boolean hasRec = count > 0;
        boolean isFut = year > currentYear;
        boolean isCurrent = year == currentYear;

        FrameLayout cell = new FrameLayout(ctx);
        View dot = new View(ctx);
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.OVAL);

        int size;
        if (isCurrent) {
            // 「今年」用描边圆环而不是实心深色点——既能标出当下所在的位置，
            // 又不会和「有记录」的墨绿实心点抢视觉重心，两者语义不再打架。
            gd.setColor(Color.TRANSPARENT);
            gd.setStroke(UiKit.dp(ctx, 1), hasRec
                    ? UiKit.getColorByName(ctx, "accent")
                    : UiKit.getColorByName(ctx, "ink_soft"));
            size = UiKit.dp(ctx, 9);
        } else if (hasRec) {
            gd.setColor(UiKit.getColorByName(ctx, "accent"));
            size = UiKit.dp(ctx, 8);
        } else if (isFut) {
            gd.setColor(UiKit.getColorByName(ctx, "muted_light"));
            dot.setAlpha(0.35f);
            size = UiKit.dp(ctx, 6);
        } else {
            gd.setColor(UiKit.getColorByName(ctx, "muted_light"));
            size = UiKit.dp(ctx, 6);
        }
        dot.setBackground(gd);

        FrameLayout.LayoutParams dlp = new FrameLayout.LayoutParams(size, size);
        dlp.gravity = Gravity.CENTER;
        cell.addView(dot, dlp);
        cell.setOnClickListener(v -> showYearPreview(year, hasRec, isFut));

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, UiKit.dp(ctx, 22), 1f);
        cell.setLayoutParams(p);
        return cell;
    }

    private View legendDot(int color, String label) {
        LinearLayout item = new LinearLayout(ctx);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(0, 0, UiKit.dp(ctx, 14), 0);

        View swatch = new View(ctx);
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.OVAL);
        gd.setColor(color);
        swatch.setBackground(gd);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(UiKit.dp(ctx, 7), UiKit.dp(ctx, 7));
        sp.setMargins(0, 0, UiKit.dp(ctx, 5), 0);
        swatch.setLayoutParams(sp);
        item.addView(swatch);

        TextView tv = new TextView(ctx);
        tv.setText(label);
        tv.setTextColor(UiKit.getColorByName(ctx, "muted"));
        tv.setTextSize(11);
        item.addView(tv);

        return item;
    }

    private View buildFooterNote() {
        TextView note = new TextView(ctx);
        note.setText("每一个点亮的点，都是你认真生活过的证明。");
        note.setTextColor(UiKit.getColorByName(ctx, "muted"));
        note.setTextSize(12);
        note.setGravity(Gravity.CENTER);
        note.setPadding(UiKit.dp(ctx, 20), UiKit.dp(ctx, 20), UiKit.dp(ctx, 20), 0);
        return note;
    }

    /**
     * 点击年份格子后，先弹出一张小卡片预览——展示这一年记录条数与一张代表性照片（如果有），
     * 让用户先"窥探"一眼再决定要不要进入日历细看，多一步情感预览，而不是硬跳转。
     */
    private void showYearPreview(int year, boolean hasRec, boolean isFuture) {
        List<MemoryRecord> yearRecords = new java.util.ArrayList<>();
        for (MemoryRecord r : repo.getAll()) {
            if (r.date.startsWith(year + "-")) yearRecords.add(r);
        }
        yearRecords.sort((a, b) -> a.date.compareTo(b.date));

        Dialog dialog = new Dialog(ctx);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(UiKit.getDrawableByName(ctx, "card_elevated"));

        // 找一张代表性照片（该年第一条带图记录）
        String coverUri = null;
        for (MemoryRecord r : yearRecords) {
            List<String> uris = r.getImageUris();
            if (!uris.isEmpty()) {
                coverUri = uris.get(0);
                break;
            }
        }
        if (coverUri != null) {
            ImageView cover = new ImageView(ctx);
            cover.setScaleType(ImageView.ScaleType.CENTER_CROP);
            cover.setImageURI(Uri.parse(coverUri));
            FrameLayout coverWrap = new FrameLayout(ctx);
            coverWrap.setClipToOutline(true);
            coverWrap.addView(cover, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, UiKit.dp(ctx, 160)));
            card.addView(coverWrap);
        }

        LinearLayout textArea = new LinearLayout(ctx);
        textArea.setOrientation(LinearLayout.VERTICAL);
        textArea.setPadding(UiKit.dp(ctx, 22), UiKit.dp(ctx, 20), UiKit.dp(ctx, 22), UiKit.dp(ctx, 20));

        TextView yearTitle = new TextView(ctx);
        yearTitle.setText(year + " 年");
        yearTitle.setTextColor(UiKit.getColorByName(ctx, "ink"));
        yearTitle.setTextSize(22);
        yearTitle.setTypeface(Typeface.DEFAULT_BOLD);
        textArea.addView(yearTitle);

        TextView desc = new TextView(ctx);
        if (isFuture) {
            desc.setText("还未到来，值得期待。");
        } else if (!hasRec) {
            desc.setText("这一年还没有留下记录。");
        } else {
            desc.setText("记录了 " + yearRecords.size() + " 条回忆");
        }
        desc.setTextColor(UiKit.getColorByName(ctx, "muted"));
        desc.setTextSize(14);
        desc.setPadding(0, UiKit.dp(ctx, 6), 0, UiKit.dp(ctx, 18));
        textArea.addView(desc);

        // 如果有代表性文字内容，摘一句放在这里增加"窥探感"
        if (!yearRecords.isEmpty()) {
            MemoryRecord sample = yearRecords.get(yearRecords.size() - 1);
            String snippet = sample.body == null || sample.body.isEmpty() ? sample.title : sample.body;
            if (snippet != null && !snippet.isEmpty()) {
                TextView quote = new TextView(ctx);
                quote.setText("“" + snippet + "”");
                quote.setTextColor(UiKit.getColorByName(ctx, "ink_soft"));
                quote.setTextSize(14);
                quote.setMaxLines(2);
                quote.setEllipsize(android.text.TextUtils.TruncateAt.END);
                quote.setPadding(0, 0, 0, UiKit.dp(ctx, 18));
                textArea.addView(quote);
            }
        }

        LinearLayout btnRow = new LinearLayout(ctx);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        TextView cancel = new TextView(ctx);
        cancel.setText("关闭");
        cancel.setTextColor(UiKit.getColorByName(ctx, "muted"));
        cancel.setTextSize(15);
        cancel.setGravity(Gravity.CENTER);
        cancel.setPadding(0, UiKit.dp(ctx, 12), 0, UiKit.dp(ctx, 12));
        cancel.setOnClickListener(v -> dialog.dismiss());
        btnRow.addView(cancel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView enter = new TextView(ctx);
        enter.setText(hasRec ? "查看这一年 ›" : (isFuture ? "" : "去记录 ›"));
        enter.setTextColor(UiKit.getColorByName(ctx, "accent"));
        enter.setTextSize(15);
        enter.setTypeface(Typeface.DEFAULT_BOLD);
        enter.setGravity(Gravity.CENTER);
        enter.setPadding(0, UiKit.dp(ctx, 12), 0, UiKit.dp(ctx, 12));
        if (!isFuture) {
            enter.setOnClickListener(v -> {
                dialog.dismiss();
                YearMonth month = hasRec
                        ? YearMonth.of(year, Integer.parseInt(yearRecords.get(0).date.substring(5, 7)))
                        : YearMonth.of(year, 1);
                LocalDate date = hasRec ? LocalDate.parse(yearRecords.get(0).date) : month.atDay(1);
                host.setVisibleMonth(month);
                host.setSelectedDate(date);
                host.openCalendar();
            });
            btnRow.addView(enter, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }
        textArea.addView(btnRow);

        card.addView(textArea);

        FrameLayout wrap = new FrameLayout(ctx);
        FrameLayout.LayoutParams cardLp = new FrameLayout.LayoutParams(
                UiKit.dp(ctx, 280), ViewGroup.LayoutParams.WRAP_CONTENT);
        wrap.addView(card, cardLp);

        dialog.setContentView(wrap);
        dialog.show();
    }
}
