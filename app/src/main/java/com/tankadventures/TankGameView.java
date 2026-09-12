package com.tankadventures;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;
import java.util.Random;

public class TankGameView extends View {
    private static final int VW = 1280;
    private static final int VH = 720;
    private static final int LEVELS = 15;
    private static final int KV2_PRICE = 500;
    private static final long PLAYER_FIRE_COOLDOWN = 2000L;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint imagePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final SharedPreferences prefs;
    private final Random random = new Random();
    private final ArrayList<Projectile> projectiles = new ArrayList<>();

    private Bitmap ms1Body;
    private Bitmap ms1Head;
    private Bitmap kv2Body;
    private Bitmap kv2Head;

    private TankPart bodyPart;
    private TankPart headPart;
    private TankType selectedTank = TankType.MS1;
    private boolean kv2Owned;
    private boolean buildMode = true;
    private boolean leftPressed;
    private boolean rightPressed;
    private boolean upPressed;
    private boolean downPressed;
    private boolean levelWon;
    private boolean levelLost;
    private int level;
    private int coins;
    private int playerHp;
    private int enemyHp;
    private int enemyMaxHp;
    private int dragPart = -1;
    private float scale = 1f;
    private float offsetX;
    private float offsetY;
    private float worldX = 450f;
    private float enemyX;
    private float headAngle = 12f;
    private float dragDx;
    private float dragDy;
    private long lastFrame;
    private long lastPlayerShot;
    private long enemyNextShot;

    private TankGameView(Context context, boolean ignored) {
        super(context);
    }

    public TankGameView(Context context) {
        super(context);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        prefs = context.getSharedPreferences("tank_adventures_clean", Context.MODE_PRIVATE);
        level = clamp(prefs.getInt("level", 1), 1, LEVELS);
        coins = Math.max(0, prefs.getInt("coins", 0));
        kv2Owned = prefs.getBoolean("kv2_owned", false);
        loadImages();
        resetTankParts();
        resetBattle();
        lastFrame = System.currentTimeMillis();
    }

    private void loadImages() {
        ms1Body = BitmapFactory.decodeResource(getResources(), getResources().getIdentifier("ms1body", "drawable", getContext().getPackageName()));
        ms1Head = BitmapFactory.decodeResource(getResources(), getResources().getIdentifier("ms1head", "drawable", getContext().getPackageName()));
        kv2Body = BitmapFactory.decodeResource(getResources(), getResources().getIdentifier("kv2body", "drawable", getContext().getPackageName()));
        kv2Head = BitmapFactory.decodeResource(getResources(), getResources().getIdentifier("kv2head", "drawable", getContext().getPackageName()));
    }

    private Bitmap bodyBitmap() {
        return selectedTank == TankType.MS1 ? ms1Body : kv2Body;
    }

    private Bitmap headBitmap() {
        return selectedTank == TankType.MS1 ? ms1Head : kv2Head;
    }

    private void resetTankParts() {
        bodyPart = new TankPart(TankPart.BODY, 470f, 430f);
        headPart = new TankPart(TankPart.HEAD, 600f, 300f);
        headPart.welded = true;
        headAngle = selectedTank == TankType.MS1 ? 10f : 12f;
    }

    private void resetBattle() {
        projectiles.clear();
        worldX = 450f;
        enemyX = 2600f + level * 120f;
        playerHp = selectedTank == TankType.MS1 ? 380 : 620;
        enemyMaxHp = 260 + level * 55;
        enemyHp = enemyMaxHp;
        levelWon = false;
        levelLost = false;
        lastPlayerShot = 0L;
        enemyNextShot = System.currentTimeMillis() + 1300L;
    }

    private void saveProgress() {
        prefs.edit()
                .putInt("level", level)
                .putInt("coins", coins)
                .putBoolean("kv2_owned", kv2Owned)
                .apply();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        long now = System.currentTimeMillis();
        float dt = Math.min(0.033f, Math.max(0f, (now - lastFrame) / 1000f));
        lastFrame = now;

        scale = Math.min(getWidth() / (float) VW, getHeight() / (float) VH);
        offsetX = (getWidth() - VW * scale) * 0.5f;
        offsetY = (getHeight() - VH * scale) * 0.5f;

        canvas.save();
        canvas.translate(offsetX, offsetY);
        canvas.scale(scale, scale);

        if (buildMode) {
            drawBuildScreen(canvas);
        } else {
            if (!levelWon && !levelLost) updateBattle(dt, now);
            drawBattleScreen(canvas);
        }

        canvas.restore();
        postInvalidateDelayed(16L);
    }

    private void drawBuildScreen(Canvas c) {
        drawBackground(c);
        text(c, "TANK ADVENTURES", 35, 42, 34, true);
        text(c, "LẮP RÁP XE", 35, 76, 22, false);
        text(c, "MÀN " + level + " / " + LEVELS + "    XU: " + coins, 880, 42, 21, true);

        panel(c, 35, 105, 840, 575, Color.argb(145, 20, 28, 34));
        text(c, "THÂN + ĐẦU XE", 65, 143, 24, true);
        text(c, "Thân chỉ là thân xe. Độ nghiêng của thân tự bám theo dốc.", 65, 174, 17, false);

        drawAssemblyPreview(c);

        panel(c, 890, 100, 1250, 570, Color.argb(230, 30, 35, 40));
        text(c, "CHỌN XE", 925, 140, 24, true);
        tankChoice(c, TankType.MS1, 915, 165, 1215, 245);
        tankChoice(c, TankType.KV2, 915, 265, 1215, 345);

        text(c, "Bộ phận của xe đã chọn", 925, 390, 19, true);
        smallPart(c, "THÂN XE", bodyBitmap(), 915, 410, 1045, 500, 0f);
        smallPart(c, "ĐẦU XE", headBitmap(), 1070, 410, 1200, 500, -headAngle);

        if (selectedTank == TankType.KV2 && !kv2Owned) {
            button(c, 925, 520, 1215, 565, "MUA KV-2 • 500 XU", Color.rgb(180, 125, 45));
        } else {
            button(c, 925, 520, 1215, 565, "ĐÃ CHỌN", Color.rgb(60, 145, 85));
        }

        button(c, 900, 605, 1065, 685, "CHIẾN ĐẤU", Color.rgb(55, 145, 75));
        button(c, 1080, 605, 1245, 685, "ĐẶT LẠI", Color.rgb(165, 75, 55));

        panel(c, 35, 590, 840, 695, Color.argb(215, 25, 25, 25));
        text(c, "Kéo ĐẦU XE vào trên THÂN XE để lắp.", 60, 625, 19, true);
        text(c, "Trong trận: ▲▼ chỉ nâng/hạ ĐẦU XE. THÂN XE không có điều khiển lên/xuống.", 60, 660, 16, false);
    }

    private void drawAssemblyPreview(Canvas c) {
        Bitmap body = bodyBitmap();
        Bitmap head = headBitmap();
        if (body != null) {
            RectF dst = fitRect(body, 180, 360, 560, 560);
            c.drawBitmap(body, null, dst, imagePaint);
        }
        if (head != null) {
            c.save();
            c.rotate(-headAngle, 600, 345);
            RectF dst = fitRect(head, 545, 250, 760, 410);
            c.drawBitmap(head, null, dst, imagePaint);
            c.restore();
        }
        if (dragPart == 1) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(5);
            paint.setColor(Color.YELLOW);
            c.drawCircle(600, 345, 80, paint);
            paint.setStyle(Paint.Style.FILL);
        }
    }

    private void tankChoice(Canvas c, TankType type, float l, float t, float r, float b) {
        boolean selected = selectedTank == type;
        boolean locked = type == TankType.KV2 && !kv2Owned;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(selected ? Color.rgb(70, 125, 80) : Color.rgb(55, 63, 68));
        c.drawRoundRect(l, t, r, b, 18, 18, paint);
        String name = type == TankType.MS1 ? "MS-1  •  MIỄN PHÍ" : (locked ? "KV-2  •  500 XU" : "KV-2  •  ĐÃ MUA");
        text(c, name, l + 18, t + 32, 20, true);
        Bitmap body = type == TankType.MS1 ? ms1Body : kv2Body;
        Bitmap head = type == TankType.MS1 ? ms1Head : kv2Head;
        if (body != null) c.drawBitmap(body, null, fitRect(body, l + 15, t + 38, l + 150, b - 8), imagePaint);
        if (head != null) c.drawBitmap(head, null, fitRect(head, l + 150, t + 38, l + 285, b - 8), imagePaint);
    }

    private void smallPart(Canvas c, String label, Bitmap bitmap, float l, float t, float r, float b, float angle) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(52, 60, 66));
        c.drawRoundRect(l, t, r, b, 14, 14, paint);
        if (bitmap != null) {
            c.save();
            c.rotate(angle, (l + r) * 0.5f, (t + b) * 0.5f);
            c.drawBitmap(bitmap, null, fitRect(bitmap, l + 8, t + 20, r - 8, b - 12), imagePaint);
            c.restore();
        }
        text(c, label, l + 8, t + 18, 13, true);
    }

    private void drawBattleScreen(Canvas c) {
        drawBackground(c);
        float camera = Math.max(0f, Math.min(levelLength() - VW, worldX - 470f));
        c.save();
        c.translate(-camera, 0);
        drawTerrain(c, camera);
        drawFinish(c);
        drawPlayer(c);
        drawEnemy(c);
        for (Projectile projectile : projectiles) projectile.draw(c);
        c.restore();

        text(c, "MÀN " + level + "/" + LEVELS, 25, 34, 23, true);
        text(c, "HP " + playerHp, 25, 63, 21, true);
        text(c, "XU " + coins, 25, 91, 19, false);
        text(c, selectedTank == TankType.MS1 ? "MS-1" : "KV-2", 25, 118, 19, true);
        text(c, "ĐẦU XE " + (int) headAngle + "°", 25, 145, 18, false);
        text(c, "ENEMY " + Math.max(0, enemyHp), 1025, 34, 22, true);
        text(c, "MÁU BẠN / ĐỊCH", 1025, 61, 15, false);

        long remain = Math.max(0L, PLAYER_FIRE_COOLDOWN - (System.currentTimeMillis() - lastPlayerShot));
        text(c, remain == 0 ? "BẮN SẴN" : "HỒI " + String.format(Locale.US, "%.1fs", remain / 1000f), 25, 173, 17, true);

        button(c, 20, 585, 115, 685, "◀", Color.rgb(55, 80, 100));
        button(c, 125, 585, 220, 685, "▶", Color.rgb(55, 80, 100));
        button(c, 230, 585, 325, 685, "▲", Color.rgb(125, 95, 55));
        button(c, 330, 585, 425, 685, "▼", Color.rgb(125, 95, 55));
        button(c, 875, 585, 1060, 685, "LẮP RÁP", Color.rgb(60, 130, 75));
        button(c, 1080, 585, 1250, 685, "BẮN", Color.rgb(185, 55, 45));

        if (levelWon) {
            panel(c, 330, 210, 950, 500, Color.argb(235, 20, 85, 35));
            text(c, "VICTORY!", 500, 295, 56, true);
            text(c, "+100 XU", 540, 345, 30, true);
            if (level < LEVELS) button(c, 490, 390, 790, 465, "MÀN TIẾP", Color.rgb(55, 145, 75));
            else text(c, "HOÀN THÀNH 15 MÀN!", 435, 410, 27, true);
        }

        if (levelLost) {
            panel(c, 330, 210, 950, 500, Color.argb(235, 95, 30, 30));
            text(c, "DEFEAT", 525, 295, 56, true);
            button(c, 490, 390, 790, 465, "CHƠI LẠI", Color.rgb(175, 75, 55));
        }
    }

    private void drawPlayer(Canvas c) {
        Bitmap body = bodyBitmap();
        Bitmap head = headBitmap();
        if (body == null || head == null) return;

        float ground = terrainY(worldX);
        float bodyWidth = 330f;
        float bodyHeight = 220f;
        float bodyTop = ground - 165f;
        float bodyCenterX = worldX;
        float bodyCenterY = bodyTop + bodyHeight * 0.55f;
        float slope = (float) Math.toDegrees(Math.atan(terrainSlope(worldX)));

        c.save();
        c.rotate(slope, bodyCenterX, ground);
        RectF bodyDst = new RectF(bodyCenterX - bodyWidth * 0.5f, bodyTop, bodyCenterX + bodyWidth * 0.5f, bodyTop + bodyHeight);
        c.drawBitmap(body, null, bodyDst, imagePaint);
        c.restore();

        float pivotX = bodyCenterX + 25f;
        float pivotY = bodyTop + 62f;
        float headWidth = selectedTank == TankType.MS1 ? 235f : 260f;
        float headHeight = selectedTank == TankType.MS1 ? 175f : 190f;

        c.save();
        c.rotate(slope - headAngle, pivotX, pivotY);
        RectF headDst = new RectF(pivotX - headWidth * 0.48f, pivotY - 35f, pivotX + headWidth * 0.52f, pivotY - 35f + headHeight);
        c.drawBitmap(head, null, headDst, imagePaint);
        c.restore();
    }

    private void drawEnemy(Canvas c) {
        float y = terrainY(enemyX);
        float x = enemyX;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(55, 0, 0, 0));
        c.drawOval(x - 150, y - 5, x + 150, y + 35, paint);
        paint.setColor(Color.rgb(70, 73, 77));
        c.drawRoundRect(x - 145, y - 70, x + 145, y - 5, 25, 25, paint);
        paint.setColor(Color.rgb(115, 120, 126));
        for (int i = 0; i < 7; i++) c.drawCircle(x - 110 + i * 36, y - 18, 14, paint);
        paint.setColor(level % 5 == 0 ? Color.rgb(150, 70, 70) : Color.rgb(125, 86, 65));
        c.drawRoundRect(x - 75, y - 135, x + 70, y - 65, 20, 20, paint);
        paint.setColor(Color.rgb(45, 48, 52));
        paint.setStrokeWidth(18);
        paint.setStrokeCap(Paint.Cap.ROUND);
        c.drawLine(x + 15, y - 102, x + 150, y - 102, paint);

        paint.setColor(Color.argb(150, 0, 0, 0));
        c.drawRoundRect(x - 135, y - 165, x + 135, y - 149, 8, 8, paint);
        paint.setColor(Color.rgb(75, 220, 90));
        float hpWidth = 270f * enemyHp / Math.max(1f, enemyMaxHp);
        c.drawRect(x - 135, y - 165, x - 135 + hpWidth, y - 149, paint);
    }

    private void drawTerrain(Canvas c, float camera) {
        float left = camera - 120f;
        float right = camera + VW + 120f;
        Path ground = new Path();
        ground.moveTo(left, terrainY(left));
        for (float x = left; x <= right; x += 20f) ground.lineTo(x, terrainY(x));
        ground.lineTo(right, VH);
        ground.lineTo(left, VH);
        ground.close();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(101, 178, 82));
        c.drawPath(ground, paint);

        Path top = new Path();
        top.moveTo(left, terrainY(left) - 18f);
        for (float x = left; x <= right; x += 20f) top.lineTo(x, terrainY(x) - 18f);
        top.lineTo(right, terrainY(right) - 18f);
        top.lineTo(left, terrainY(left) - 18f);
        top.close();
        paint.setColor(Color.rgb(177, 220, 111));
        c.drawPath(top, paint);
    }

    private void drawFinish(Canvas c) {
        float x = levelLength() - 180f;
        float y = terrainY(x);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.DKGRAY);
        c.drawRect(x, y - 180f, x + 9f, y, paint);
        paint.setColor(Color.RED);
        Path flag = new Path();
        flag.moveTo(x + 9f, y - 180f);
        flag.lineTo(x + 120f, y - 150f);
        flag.lineTo(x + 9f, y - 120f);
        flag.close();
        c.drawPath(flag, paint);
        text(c, "FINISH", x - 15, y - 195, 19, true);
    }

    private void updateBattle(float dt, long now) {
        float speed = selectedTank == TankType.MS1 ? 145f : 118f;
        if (leftPressed) worldX -= speed * dt;
        if (rightPressed) worldX += speed * dt;
        worldX = Math.max(160f, Math.min(levelLength() - 320f, worldX));

        if (upPressed) headAngle = Math.min(38f, headAngle + 48f * dt);
        if (downPressed) headAngle = Math.max(-4f, headAngle - 48f * dt);

        enemyX -= 8f * dt;
        enemyX = Math.max(worldX + 480f, enemyX);

        if (now >= enemyNextShot && enemyHp > 0 && playerHp > 0) {
            fireEnemyShot();
            enemyNextShot = now + Math.max(900L, 1900L - level * 45L);
        }

        for (Projectile projectile : projectiles) {
            if (projectile.dead) continue;
            projectile.vy += 760f * dt;
            projectile.x += projectile.vx * dt;
            projectile.y += projectile.vy * dt;
            projectile.life -= dt;

            if (projectile.y >= terrainY(projectile.x)) {
                projectile.dead = true;
                continue;
            }
            if (projectile.life <= 0f) {
                projectile.dead = true;
                continue;
            }

            if (projectile.enemy) {
                if (Math.abs(projectile.x - worldX) < 165f && projectile.y > terrainY(worldX) - 165f && projectile.y < terrainY(worldX) + 20f) {
                    projectile.dead = true;
                    playerHp = Math.max(0, playerHp - projectile.damage);
                }
            } else if (enemyHp > 0) {
                float targetY = terrainY(enemyX) - 90f;
                if (Math.abs(projectile.x - enemyX) < 155f && Math.abs(projectile.y - targetY) < 120f) {
                    projectile.dead = true;
                    enemyHp = Math.max(0, enemyHp - projectile.damage);
                }
            }
        }

        Iterator<Projectile> iterator = projectiles.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().dead) iterator.remove();
        }

        if (enemyHp <= 0 && !levelWon) {
            levelWon = true;
            coins += 100;
            saveProgress();
        }
        if (playerHp <= 0) levelLost = true;
    }

    private void fireEnemyShot() {
        float startX = enemyX - 40f;
        float startY = terrainY(enemyX) - 110f;
        float dx = worldX - startX;
        float dy = (terrainY(worldX) - 100f) - startY;
        float t = Math.max(0.45f, Math.abs(dx) / 600f);
        float angle = (float) Math.atan2((dy - 380f * t * t) / t, dx);
        projectiles.add(new Projectile(startX, startY, angle, 600f, 28, true));
    }

    private void firePlayer() {
        long now = System.currentTimeMillis();
        if (now - lastPlayerShot < PLAYER_FIRE_COOLDOWN || levelWon || levelLost || enemyHp <= 0) return;

        float slope = (float) Math.atan(terrainSlope(worldX));
        float angle = slope - (float) Math.toRadians(headAngle);
        float pivotX = worldX + 25f;
        float pivotY = terrainY(worldX) - 105f;
        float muzzleX = pivotX + 150f * (float) Math.cos(angle);
        float muzzleY = pivotY + 150f * (float) Math.sin(angle);

        projectiles.add(new Projectile(muzzleX, muzzleY, angle, 700f, selectedTank == TankType.MS1 ? 34 : 50, false));
        lastPlayerShot = now;
    }

    private float terrainY(float x) {
        float phase = level * 0.63f;
        return 465f
                + 52f * (float) Math.sin(x / 265f + phase)
                + 25f * (float) Math.sin(x / 110f + level)
                + 15f * (float) Math.sin(x / 56f + 2f);
    }

    private float terrainSlope(float x) {
        return (terrainY(x + 2f) - terrainY(x - 2f)) / 4f;
    }

    private float levelLength() {
        return 4400f + level * 250f;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = (event.getX() - offsetX) / scale;
        float y = (event.getY() - offsetY) / scale;

        if (buildMode) return handleBuildTouch(event, x, y);
        return handleBattleTouch(event, x, y);
    }

    private boolean handleBuildTouch(MotionEvent event, float x, float y) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            if (x >= 900 && x <= 1065 && y >= 590) {
                buildMode = false;
                resetBattle();
                return true;
            }
            if (x >= 1080 && y >= 590) {
                selectedTank = TankType.MS1;
                resetTankParts();
                return true;
            }
            if (x >= 915 && x <= 1215 && y >= 165 && y <= 245) {
                selectedTank = TankType.MS1;
                resetTankParts();
                return true;
            }
            if (x >= 915 && x <= 1215 && y >= 265 && y <= 345) {
                if (!kv2Owned) {
                    if (coins >= KV2_PRICE) {
                        coins -= KV2_PRICE;
                        kv2Owned = true;
                        saveProgress();
                    } else {
                        return true;
                    }
                }
                selectedTank = TankType.KV2;
                resetTankParts();
                return true;
            }
            if (x >= 1065 && x <= 1215 && y >= 520 && y <= 570 && selectedTank == TankType.KV2 && !kv2Owned) {
                if (coins >= KV2_PRICE) {
                    coins -= KV2_PRICE;
                    kv2Owned = true;
                    saveProgress();
                }
                return true;
            }

            if (headPart.welded && Math.hypot(x - headPart.x, y - headPart.y) < 100f) {
                dragPart = 1;
                dragDx = headPart.x - x;
                dragDy = headPart.y - y;
                return true;
            }
        }

        if (action == MotionEvent.ACTION_MOVE && dragPart == 1) {
            headPart.x = x + dragDx;
            headPart.y = y + dragDy;
            headPart.welded = false;
            invalidate();
            return true;
        }

        if (action == MotionEvent.ACTION_UP && dragPart == 1) {
            dragPart = -1;
            if (Math.hypot(headPart.x - 600f, headPart.y - 345f) < 125f) {
                headPart.x = 600f;
                headPart.y = 345f;
                headPart.welded = true;
            }
            invalidate();
            return true;
        }
        return true;
    }

    private boolean handleBattleTouch(MotionEvent event, float x, float y) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
            leftPressed = x < 120f && y > 570f;
            rightPressed = x >= 120f && x < 225f && y > 570f;
            upPressed = x >= 225f && x < 325f && y > 570f;
            downPressed = x >= 325f && x < 425f && y > 570f;

            if (action == MotionEvent.ACTION_DOWN && x > 1075f && y > 570f) {
                firePlayer();
                return true;
            }
            if (action == MotionEvent.ACTION_DOWN && x > 870f && x < 1065f && y > 570f) {
                buildMode = true;
                return true;
            }
            if (action == MotionEvent.ACTION_DOWN && levelWon && level < LEVELS && x > 480f && x < 800f && y > 380f && y < 480f) {
                level++;
                saveProgress();
                resetBattle();
                return true;
            }
            if (action == MotionEvent.ACTION_DOWN && levelLost && x > 480f && x < 800f && y > 380f && y < 480f) {
                resetBattle();
                return true;
            }
            return true;
        }

        leftPressed = false;
        rightPressed = false;
        upPressed = false;
        downPressed = false;
        return true;
    }

    private RectF fitRect(Bitmap bitmap, float left, float top, float right, float bottom) {
        float boxW = right - left;
        float boxH = bottom - top;
        float ratio = Math.min(boxW / Math.max(1, bitmap.getWidth()), boxH / Math.max(1, bitmap.getHeight()));
        float w = bitmap.getWidth() * ratio;
        float h = bitmap.getHeight() * ratio;
        float x = (left + right - w) * 0.5f;
        float y = (top + bottom - h) * 0.5f;
        return new RectF(x, y, x + w, y + h);
    }

    private void drawBackground(Canvas c) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(67, 151, 196));
        c.drawRect(0, 0, VW, VH, paint);
        paint.setColor(Color.argb(60, 255, 255, 255));
        c.drawCircle(170, 105, 38, paint);
        c.drawCircle(218, 92, 50, paint);
        c.drawCircle(270, 108, 34, paint);
        c.drawCircle(865, 120, 38, paint);
        c.drawCircle(915, 108, 52, paint);
        c.drawCircle(970, 125, 33, paint);
    }

    private void panel(Canvas c, float l, float t, float r, float b, int color) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        c.drawRoundRect(l, t, r, b, 22, 22, paint);
    }

    private void button(Canvas c, float l, float t, float r, float b, String label, int color) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        c.drawRoundRect(l, t, r, b, 18, 18, paint);
        paint.setColor(Color.WHITE);
        paint.setTextSize(21);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        float width = paint.measureText(label);
        c.drawText(label, (l + r - width) * 0.5f, t + (b - t) * 0.5f + 8f, paint);
    }

    private void text(Canvas c, String value, float x, float y, float size, boolean bold) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setTextSize(size);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL));
        c.drawText(value, x, y, paint);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private enum TankType {
        MS1,
        KV2
    }

    private static class TankPart {
        static final int BODY = 0;
        static final int HEAD = 1;
        final int type;
        float x;
        float y;
        boolean welded;

        TankPart(int type, float x, float y) {
            this.type = type;
            this.x = x;
            this.y = y;
        }
    }

    private static class Projectile {
        float x;
        float y;
        float vx;
        float vy;
        float life = 5f;
        final int damage;
        final boolean enemy;
        boolean dead;

        Projectile(float x, float y, float angle, float speed, int damage, boolean enemy) {
            this.x = x;
            this.y = y;
            this.vx = (float) Math.cos(angle) * speed;
            this.vy = (float) Math.sin(angle) * speed;
            this.damage = damage;
            this.enemy = enemy;
        }

        void draw(Canvas c) {
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setColor(Color.argb(150, 255, 185, 60));
            c.drawCircle(x, y, 15, p);
            p.setColor(Color.rgb(255, 235, 100));
            c.drawCircle(x, y, 8, p);
        }
    }
}
