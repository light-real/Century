package com.bainian.memory.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;

import com.bainian.memory.data.RecordRepository;
import com.bainian.memory.util.UiKit;
import com.bainian.memory.util.WidgetFactory;
import com.bainian.memory.view.ProfileAvatarView;
import com.bainian.memory.view.SettingsIconView;

import java.time.LocalDate;

/**
 * 「我的」页：出生年份设置、统计信息、关于与系统设置入口。
 * 现在作为次级页面使用（从首页右上角头像进入），顶部带返回箭头。
 */
public class ProfilePage {
    private final Context ctx;
    private final RecordRepository repo;
    private final PageHost host;
    private final Runnable onBack;
    private Runnable rebuild = () -> {};

    public ProfilePage(Context ctx, RecordRepository repo, PageHost host) {
        this(ctx, repo, host, null);
    }

    public ProfilePage(Context ctx, RecordRepository repo, PageHost host, Runnable onBack) {
        this.ctx = ctx;
        this.repo = repo;
        this.host = host;
        this.onBack = onBack;
    }

    /** 供宿主容器传入「原地重建整页」的回调，更换头像后调用，避免影响外层导航状态 */
    public void setRebuildCallback(Runnable rebuild) {
        this.rebuild = rebuild;
    }

    public View build() {
        ScrollView scroll = new ScrollView(ctx);
        scroll.setClipToPadding(false);
        LinearLayout body = WidgetFactory.pageBody(ctx);
        body.setPadding(UiKit.dp(ctx, 20), UiKit.dp(ctx, 16), UiKit.dp(ctx, 20), UiKit.dp(ctx, 32));
        scroll.addView(body);

        // 返回箭头
        TextView back = new TextView(ctx);
        back.setText("‹ 返回");
        back.setTextSize(16);
        back.setTextColor(UiKit.getColorByName(ctx, "ink"));
        back.setPadding(0, 0, 0, UiKit.dp(ctx, 14));
        back.setOnClickListener(v -> {
            if (onBack != null) onBack.run();
        });
        body.addView(back);

        int birthYear = repo.getBirthYear();
        int age = Math.min(Math.max(0, LocalDate.now().getYear() - birthYear), 100);
        int totalDays = repo.countRecordedDays();
        int totalRecords = repo.getAll().size();
        int recordedYears = 0;
        for (int i = 0; i < 100; i++) {
            if (repo.recordCountForYear(birthYear + i) > 0) recordedYears++;
        }

        body.addView(buildHeaderCard(age, totalDays, totalRecords));
        body.addView(buildCenturyEntryCard(age, recordedYears));

        // ── 统计卡片行 ──
        // 情感优先于效率：还没有任何记录时，不用「0/0/0」的仪表盘式呈现制造空落感，
        // 而是换成一句温和的邀请文案；等用户真正记录起来，数字本身就会变成值得回看的印记。
        boolean hasAnyRecord = totalRecords > 0;
        if (hasAnyRecord) {
            LinearLayout statsRow = new LinearLayout(ctx);
            statsRow.setOrientation(LinearLayout.HORIZONTAL);
            statsRow.setLayoutParams(WidgetFactory.fullWidthMargin(ctx, 0, UiKit.dp(ctx, 20)));
            body.addView(statsRow);

            statsRow.addView(statCard("记录天数", String.valueOf(totalDays), "天", UiKit.getColorByName(ctx, "accent")));
            statsRow.addView(statSpacer());
            statsRow.addView(statCard("记忆条数", String.valueOf(totalRecords), "条", 0xFFE8A838));
            statsRow.addView(statSpacer());
            statsRow.addView(statCard("活跃年份", String.valueOf(recordedYears), "年", 0xFF5B9BD5));
        } else {
            body.addView(emptyInviteCard());
        }

        // ── 个人信息分组 ──
        body.addView(sectionHeader("个人信息"));
        body.addView(settingsItem(SettingsIconView.Kind.BIRTH, "出生年份",
                v -> RecordActions.editBirthYear(ctx, repo, host)));

        // ── 关于分组 ──
        body.addView(sectionHeader("关于"));
        body.addView(settingsItem(SettingsIconView.Kind.ABOUT, "关于百年", v -> new AlertDialog.Builder(ctx)
                .setTitle("百年")
                .setMessage("记录你认真生活过的每一天。\n\n把每天的小事，放进一生的日历里，留到很久以后再翻出来看看。")
                .setPositiveButton("知道了", null).show()));
        body.addView(settingsItem(SettingsIconView.Kind.SYSTEM, "通知与权限", v -> ctx.startActivity(
                new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + ctx.getPackageName())))));

        // ── 底部落款 ──
        Space sp = new Space(ctx);
        sp.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, UiKit.dp(ctx, 24)));
        body.addView(sp);

        TextView version = new TextView(ctx);
        version.setText("百年 · 认真记录的每一天，都算数");
        version.setTextColor(UiKit.getColorByName(ctx, "muted"));
        version.setTextSize(12);
        version.setGravity(Gravity.CENTER);
        body.addView(version, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        return scroll;
    }

    /**
     * 「百年」入口卡片——用一句感性文案邀请用户主动点进去做一次「回望人生」的探访，
     * 而不是把它塞进普通设置列表里。这里刻意不用「26/100」这种数字比值/进度条式的展示，
     * 因为「百年」是低频、情感浓度很高的页面，用 KPI 完成度的语言去呈现人生，
     * 会削弱它本该有的仪式感——含蓄地留一句「第 26 个年头」式的措辞，让用户自己点进去看。
     */
    private View buildCenturyEntryCard(int age, int recordedYears) {
        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(UiKit.dp(ctx, 22), UiKit.dp(ctx, 20), UiKit.dp(ctx, 20), UiKit.dp(ctx, 20));
        card.setBackground(UiKit.getDrawableByName(ctx, "card_elevated"));
        card.setLayoutParams(WidgetFactory.fullWidthMargin(ctx, 0, UiKit.dp(ctx, 14)));
        card.setOnClickListener(v -> host.openCentury());

        LinearLayout textArea = new LinearLayout(ctx);
        textArea.setOrientation(LinearLayout.VERTICAL);

        TextView title = new TextView(ctx);
        title.setText("百年");
        title.setTextColor(UiKit.getColorByName(ctx, "ink"));
        title.setTextSize(17);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        textArea.addView(title);

        TextView desc = new TextView(ctx);
        desc.setText(recordedYears > 0
                ? "看看这一生的样子，已留下 " + recordedYears + " 个年份的痕迹"
                : "看看这一生的样子，从这一年开始记起");
        desc.setTextColor(UiKit.getColorByName(ctx, "muted"));
        desc.setTextSize(12);
        desc.setPadding(0, UiKit.dp(ctx, 4), 0, 0);
        textArea.addView(desc);

        card.addView(textArea, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView ageBadge = new TextView(ctx);
        ageBadge.setText("第 " + age + " 年");
        ageBadge.setTextColor(UiKit.getColorByName(ctx, "accent"));
        ageBadge.setTextSize(15);
        ageBadge.setTypeface(Typeface.DEFAULT_BOLD);
        card.addView(ageBadge);

        TextView arrow = new TextView(ctx);
        arrow.setText("›");
        arrow.setTextColor(UiKit.getColorByName(ctx, "muted"));
        arrow.setTextSize(20);
        arrow.setPadding(UiKit.dp(ctx, 8), 0, 0, 0);
        card.addView(arrow);

        return card;
    }

    private View buildHeaderCard(int age, int totalDays, int totalRecords) {
        LinearLayout headerCard = new LinearLayout(ctx);
        headerCard.setOrientation(LinearLayout.VERTICAL);
        headerCard.setGravity(Gravity.CENTER);
        headerCard.setPadding(UiKit.dp(ctx, 28), UiKit.dp(ctx, 32), UiKit.dp(ctx, 28), UiKit.dp(ctx, 28));
        headerCard.setBackground(UiKit.getDrawableByName(ctx, "card_elevated"));
        headerCard.setLayoutParams(WidgetFactory.fullWidthMargin(ctx, 0, UiKit.dp(ctx, 20)));

        // 圆形头像——点击可更换，选择完成后原地刷新整页
        FrameLayout avatarWrap = new FrameLayout(ctx);
        ProfileAvatarView avatar = new ProfileAvatarView(ctx);
        avatar.setSizeAndColor(UiKit.dp(ctx, 72), UiKit.getColorByName(ctx, "accent"));
        avatar.setAvatarUri(ctx, repo.getAvatarUri());
        avatarWrap.addView(avatar, new FrameLayout.LayoutParams(UiKit.dp(ctx, 72), UiKit.dp(ctx, 72)));

        // 右下角小相机角标，提示「可点击更换」——用手绘矢量图标，
        // 与底部导航、设置列表统一的几何图标语言保持一致，而不是借用系统 emoji。
        FrameLayout camBadgeWrap = new FrameLayout(ctx);
        camBadgeWrap.setBackground(UiKit.getDrawableByName(ctx, "card_elevated"));
        int badgeSize = UiKit.dp(ctx, 24);
        FrameLayout.LayoutParams badgeLp = new FrameLayout.LayoutParams(badgeSize, badgeSize);
        badgeLp.gravity = Gravity.END | Gravity.BOTTOM;
        SettingsIconView camIcon = new SettingsIconView(ctx);
        camIcon.setKind(SettingsIconView.Kind.CAMERA);
        camIcon.setIconColor(UiKit.getColorByName(ctx, "accent"));
        int camIconSize = UiKit.dp(ctx, 14);
        FrameLayout.LayoutParams camIconLp = new FrameLayout.LayoutParams(camIconSize, camIconSize);
        camIconLp.gravity = Gravity.CENTER;
        camBadgeWrap.addView(camIcon, camIconLp);
        avatarWrap.addView(camBadgeWrap, badgeLp);

        avatarWrap.setOnClickListener(v -> host.pickAvatarImage(uri -> {
            if (uri != null && !uri.isEmpty()) {
                repo.setAvatarUri(uri);
                rebuild.run();
            }
        }));
        headerCard.addView(avatarWrap, new LinearLayout.LayoutParams(UiKit.dp(ctx, 72), UiKit.dp(ctx, 72)));

        // 问候语——用日常说话的口吻，而不是「生命记录者」这类身份标签，
        // 呼应首页「下午好」式的陪伴感文案。
        TextView name = new TextView(ctx);
        name.setText(greetingText(totalDays));
        name.setTextColor(UiKit.getColorByName(ctx, "ink"));
        name.setTextSize(20);
        name.setTypeface(Typeface.DEFAULT_BOLD);
        name.setPadding(0, UiKit.dp(ctx, 14), 0, UiKit.dp(ctx, 6));
        headerCard.addView(name);

        // 统计行——已有记录时才展示，避免「0 天 · 0 条」的空落感
        if (totalDays > 0) {
            TextView stat = new TextView(ctx);
            stat.setText("已经陪自己走过 " + totalDays + " 天  ·  " + totalRecords + " 条记忆");
            stat.setTextColor(UiKit.getColorByName(ctx, "muted"));
            stat.setTextSize(13);
            stat.setGravity(Gravity.CENTER);
            headerCard.addView(stat);
        } else {
            TextView stat = new TextView(ctx);
            stat.setText("今年 " + age + " 岁，日子才刚刚开始被记住");
            stat.setTextColor(UiKit.getColorByName(ctx, "muted"));
            stat.setTextSize(13);
            stat.setGravity(Gravity.CENTER);
            headerCard.addView(stat);
        }

        return headerCard;
    }

    /** 头部问候语：已有记录时更含蓄地带出陪伴时长，避免用「生命记录者」这类冷冰冰的身份标签 */
    private String greetingText(int totalDays) {
        if (totalDays <= 0) return "你好，欢迎回到这里";
        return "你好，又见面了";
    }

    /** 尚无任何记录时的邀请卡片，代替「0/0/0」统计卡片行，延续「情感优先于效率」的原则 */
    private View emptyInviteCard() {
        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(UiKit.dp(ctx, 20), UiKit.dp(ctx, 22), UiKit.dp(ctx, 20), UiKit.dp(ctx, 22));
        card.setBackground(UiKit.getDrawableByName(ctx, "card_elevated"));
        card.setLayoutParams(WidgetFactory.fullWidthMargin(ctx, 0, UiKit.dp(ctx, 20)));

        TextView tv = new TextView(ctx);
        tv.setText("还没有留下第一条记忆\n从今天开始，慢慢攒起来");
        tv.setTextColor(UiKit.getColorByName(ctx, "muted"));
        tv.setTextSize(13);
        tv.setGravity(Gravity.CENTER);
        tv.setLineSpacing(UiKit.dp(ctx, 4), 1f);
        card.addView(tv);

        return card;
    }

    // ── 统计小卡片 ──
    private View statCard(String label, String value, String unit, int accentColor) {
        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(UiKit.dp(ctx, 12), UiKit.dp(ctx, 18), UiKit.dp(ctx, 12), UiKit.dp(ctx, 18));
        card.setBackground(UiKit.getDrawableByName(ctx, "card_elevated"));

        TextView val = new TextView(ctx);
        val.setText(value);
        val.setTextColor(accentColor);
        val.setTextSize(28);
        val.setTypeface(Typeface.DEFAULT_BOLD);
        val.setGravity(Gravity.CENTER);
        card.addView(val);

        TextView unitTv = new TextView(ctx);
        unitTv.setText(unit);
        unitTv.setTextColor(accentColor);
        unitTv.setTextSize(10);
        unitTv.setPadding(0, UiKit.dp(ctx, 2), 0, UiKit.dp(ctx, 4));
        unitTv.setGravity(Gravity.CENTER);
        card.addView(unitTv);

        TextView lb = new TextView(ctx);
        lb.setText(label);
        lb.setTextColor(UiKit.getColorByName(ctx, "muted"));
        lb.setTextSize(11);
        lb.setGravity(Gravity.CENTER);
        card.addView(lb);

        card.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return card;
    }

    private View statSpacer() {
        Space s = new Space(ctx);
        s.setLayoutParams(new LinearLayout.LayoutParams(UiKit.dp(ctx, 10),
                ViewGroup.LayoutParams.MATCH_PARENT));
        return s;
    }

    // ── 分组标题 ──
    private TextView sectionHeader(String title) {
        TextView tv = new TextView(ctx);
        tv.setText(title);
        tv.setTextColor(UiKit.getColorByName(ctx, "ink"));
        tv.setTextSize(15);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setPadding(0, UiKit.dp(ctx, 8), 0, UiKit.dp(ctx, 12));
        tv.setLayoutParams(WidgetFactory.fullWidthMargin(ctx, 0, 0));
        return tv;
    }

    private View settingsItem(SettingsIconView.Kind kind, String label, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(UiKit.dp(ctx, 18), UiKit.dp(ctx, 16), UiKit.dp(ctx, 18), UiKit.dp(ctx, 16));
        row.setBackground(UiKit.getDrawableByName(ctx, "card_elevated"));
        row.setOnClickListener(listener);

        // 自定义图标
        SettingsIconView ic = new SettingsIconView(ctx);
        ic.setKind(kind);
        ic.setIconColor(UiKit.getColorByName(ctx, "accent"));
        int iconSize = UiKit.dp(ctx, 22);
        ic.setLayoutParams(new LinearLayout.LayoutParams(iconSize, iconSize));
        row.addView(ic);
        // 间距
        Space gap = new Space(ctx);
        gap.setLayoutParams(new LinearLayout.LayoutParams(UiKit.dp(ctx, 14), 0));
        row.addView(gap);

        TextView lb = new TextView(ctx);
        lb.setText(label);
        lb.setTextColor(UiKit.getColorByName(ctx, "ink"));
        lb.setTextSize(15);
        row.addView(lb, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView ar = new TextView(ctx);
        ar.setText("›");
        ar.setTextColor(UiKit.getColorByName(ctx, "muted"));
        ar.setTextSize(20);
        row.addView(ar);

        row.setLayoutParams(WidgetFactory.fullWidthMargin(ctx, 0, UiKit.dp(ctx, 10)));
        return row;
    }
}
