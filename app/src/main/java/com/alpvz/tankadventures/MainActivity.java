package com.alpvz.tankadventures;

import android.app.Activity;
import android.os.Bundle;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class MainActivity extends Activity {
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(new GameView(this));
    }

    static class GameView extends View {
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Path path = new Path();
        final Random random = new Random();
        final SharedPreferences prefs;

        final int[] HP = {500, 800, 1100, 1500};
        final int[] COST = {0, 1000, 2000, 3000};
        final float[] SPEED = {4.4f, 4.8f, 5.2f, 5.7f};

        int coins, owned, selected, level, wins;
        int hp, chassis, armor, wheels, motor;
        float x, y, vx, vy, cam, tilt;
        boolean playing, win, lose, shop;
        long start, lastSelfRepair, lastImpact;

        float joystickX = 105, joystickY;
        float knobX = 105, knobY;

        final ArrayList<Rival> rivals = new ArrayList<>();
        final ArrayList<Part> looseParts = new ArrayList<>();

        GameView(Context c) {
            super(c);
            prefs = c.getSharedPreferences("tank_adventures", 0);
            coins = prefs.getInt("coins", 0);
            owned = prefs.getInt("owned", 0);
            selected = Math.max(0, Math.min(3, prefs.getInt("selected", 0)));
            level = Math.max(1, Math.min(15, prefs.getInt("level", 1)));
            wins = prefs.getInt("wins", 0);
            p.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        }

        void save() {
            prefs.edit().putInt("coins", coins).putInt("owned", owned)
                    .putInt("selected", selected).putInt("level", level)
                    .putInt("wins", wins).apply();
        }

        @Override protected void onDraw(Canvas c) {
            if (playing) {
                update();
                drawGame(c);
            } else {
                drawMenu(c);
            }
            postInvalidateDelayed(16);
        }

        void startLevel() {
            playing = true;
            win = false;
            lose = false;
            shop = false;
            hp = HP[selected];
            chassis = 100;
            armor = 100;
            wheels = 100;
            motor = 100;
            x = 120;
            y = ground(x) - 45;
            vx = 0;
            vy = 0;
            cam = 0;
            tilt = 0;
            rivals.clear();
            looseParts.clear();
            random.setSeed(level * 997L + selected * 71L);
            for (int i = 0; i < 8 + level / 3; i++) {
                Rival r = new Rival();
                r.x = 900 + i * 430 + random.nextInt(180);
                r.speed = 1.0f + level * 0.03f;
                r.size = 0.90f + Math.min(0.55f, level * 0.035f + (i % 3) * 0.05f);
                r.durability = 100 + level * 9 + (i % 3) * 35;
                r.maxDurability = r.durability;
                rivals.add(r);
            }
            start = System.currentTimeMillis();
            lastSelfRepair = start;
            lastImpact = 0;
        }

        float ground(float wx) {
            return getHeight() * .69f
                    + (float)Math.sin(wx * .0047f) * 65f
                    + (float)Math.sin(wx * .011f + level) * 24f;
        }

        void update() {
            long now = System.currentTimeMillis();
            float dt = Math.min(2.0f, Math.max(.55f, (now - start) / 16.666f + .5f));

            if (hp <= 0 || chassis <= 0) {
                lose = true;
                playing = false;
                return;
            }

            if (now - start > 60000) {
                win = true;
                playing = false;
                wins++;
                coins += 100;
                if (wins >= 2) {
                    wins = 0;
                    if (level < 15) level++;
                }
                save();
                return;
            }

            // Smooth Super-Tank-Rumble-style driving feel.
            float input = (knobX - joystickX) / 55f;
            if (Math.abs(input) < .08f) input = 0;
            float maxSpeed = SPEED[selected]
                    * (motor < 40 ? .62f : 1f)
                    * (wheels < 40 ? .70f : 1f);
            vx += input * .12f * dt;
            if (Math.abs(input) < .08f) vx *= .972f;
            if (vx > maxSpeed) vx = maxSpeed;
            if (vx < -maxSpeed * .55f) vx = -maxSpeed * .55f;
            x += vx * dt * 1.7f;
            if (x < 60) x = 60;

            float targetY = ground(x) - (wheels <= 0 ? 36 : 45);
            vy += .48f * dt;
            y += vy * dt;
            if (y >= targetY) {
                float impact = Math.abs(vy);
                if (impact > 6 && now - lastImpact > 300) applyBreakdown((int)Math.min(24, impact * 1.8f));
                y = targetY;
                vy = -Math.min(3.8f, impact * .18f);
            }

            tilt = (float)Math.atan2(ground(x + 25) - ground(x - 25), 50);
            cam += (x - getWidth() * .30f - cam) * .11f;
            if (cam < 0) cam = 0;

            // Passive self-repair: slowly restores damaged modules, never above 100.
            if (now - lastSelfRepair >= 4500) {
                if (hp < HP[selected]) hp = Math.min(HP[selected], hp + 12);
                chassis = Math.min(100, chassis + 8);
                armor = Math.min(100, armor + 10);
                wheels = Math.min(100, wheels + 6);
                motor = Math.min(100, motor + 6);
                lastSelfRepair = now;
            }

            // Rival vehicles are non-projectile moving obstacles. Collisions can knock parts loose.
            for (Rival r : rivals) {
                if (r.removed) continue;
                float dx = x - r.x;
                float dy = y - (ground(r.x) - 43 * r.size);
                float d = Math.max(1f, (float)Math.hypot(dx, dy));
                if (Math.abs(dx) < 340) r.x += (dx / d) * r.speed * dt;
                if (d < 70 * r.size && now - r.lastHit > 700) {
                    int selfLoss = 5 + level / 3;
                    int rivalLoss = 8 + level / 2;
                    hp = Math.max(0, hp - selfLoss);
                    applyBreakdown(selfLoss + 2);
                    r.durability -= rivalLoss;
                    r.lastHit = now;
                    if (r.durability <= 0) {
                        r.removed = true;
                        coins += 10;
                        save();
                    }
                    vx += dx >= 0 ? 2.0f : -2.0f;
                    vy = -3.2f;
                }
            }

            // Loose modules settle back onto the ground.
            Iterator<Part> it = looseParts.iterator();
            while (it.hasNext()) {
                Part part = it.next();
                part.vy += .36f * dt;
                part.x += part.vx * dt;
                part.y += part.vy * dt;
                float gy = ground(part.x) - 8;
                if (part.y >= gy) {
                    part.y = gy;
                    part.vy *= -.25f;
                    part.vx *= .85f;
                    if (Math.abs(part.vx) < .12f && Math.abs(part.vy) < .12f) part.sleep = true;
                }
                if (part.x < cam - 200 || part.x > cam + getWidth() + 300) it.remove();
            }
        }

        void applyBreakdown(int amount) {
            long now = System.currentTimeMillis();
            lastImpact = now;
            int a = Math.max(1, amount);
            chassis = Math.max(0, chassis - a);
            armor = Math.max(0, armor - a / 2);
            wheels = Math.max(0, wheels - a / 3);
            motor = Math.max(0, motor - a / 4);
            hp = Math.max(0, hp - Math.max(1, a / 4));

            if (armor <= 0 && random.nextInt(100) < 20) dropPart(Part.ARMOR);
            if (wheels <= 0 && random.nextInt(100) < 18) dropPart(Part.WHEEL);
            if (motor <= 0 && random.nextInt(100) < 15) dropPart(Part.MOTOR);
        }

        void dropPart(int type) {
            for (Part p : looseParts) if (p.type == type && Math.abs(p.x - x) < 40) return;
            Part part = new Part();
            part.type = type;
            part.x = x + (random.nextBoolean() ? 18 : -18);
            part.y = y - 10;
            part.vx = (random.nextBoolean() ? 1 : -1) * (1.2f + random.nextFloat() * 1.7f);
            part.vy = -3.0f - random.nextFloat() * 1.5f;
            looseParts.add(part);
        }

        void repairAll() {
            if (!playing) return;
            if (coins >= 25) {
                coins -= 25;
                hp = Math.min(HP[selected], hp + 90);
                chassis = Math.min(100, chassis + 30);
                armor = Math.min(100, armor + 35);
                wheels = Math.min(100, wheels + 25);
                motor = Math.min(100, motor + 25);
                save();
            }
        }

        void drawGame(Canvas c) {
            c.drawColor(Color.rgb(82, 165, 220));
            drawClouds(c);

            p.setColor(Color.rgb(125, 150, 86));
            path.reset();
            path.moveTo(0, getHeight());
            for (int i = 0; i <= getWidth(); i += 8) path.lineTo(i, ground(cam + i));
            path.lineTo(getWidth(), getHeight());
            path.close();
            c.drawPath(path, p);

            for (Rival r : rivals) if (!r.removed) drawRival(c, r);
            for (Part part : looseParts) drawPart(c, part);
            drawVehicle(c, x - cam, y, selected, 1.25f, tilt, true);
            drawHud(c);
            drawControls(c);
        }

        void drawClouds(Canvas c) {
            p.setColor(Color.argb(145, 255, 255, 255));
            for (int i = -1; i < 6; i++) {
                float cx = i * 240 - (cam * .15f % 240);
                c.drawCircle(cx, 90 + (i % 2) * 28, 26, p);
                c.drawCircle(cx + 28, 94 + (i % 2) * 28, 34, p);
                c.drawCircle(cx + 60, 98 + (i % 2) * 28, 24, p);
            }
        }

        void drawVehicle(Canvas c, float cx, float cy, int type, float scale, float angle, boolean player) {
            c.save();
            c.rotate((float)Math.toDegrees(angle), cx, cy);
            float w = 84 * scale;
            float h = 44 * scale;

            // Frame / chassis.
            p.setColor(Color.rgb(35, 35, 38));
            c.drawRoundRect(new RectF(cx - w / 2 - 4, cy - h / 2, cx + w / 2 + 4, cy + h / 2 + 8), 10, 10, p);
            p.setColor(player ? Color.rgb(42, 145, 68) : Color.rgb(105, 107, 110));
            c.drawRoundRect(new RectF(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2), 8, 8, p);

            // Removable armor plates.
            if (player && armor > 0) {
                p.setColor(Color.rgb(26, 93, 43));
                c.drawRoundRect(new RectF(cx - w / 2 + 5, cy - h / 2 - 8, cx + w / 2 - 8, cy - h / 2 + 4), 5, 5, p);
                if (armor < 40) p.setColor(Color.argb(110, 255, 210, 70));
            }

            // Four wheels; missing wheels visibly change the vehicle.
            p.setColor(Color.DKGRAY);
            int wheelCount = player ? (wheels <= 0 ? 2 : 4) : 4;
            for (int i = 0; i < wheelCount; i++) {
                float wx = cx - w * .31f + i * w * .21f;
                c.drawCircle(wx, cy + h * .52f, 10 * scale, p);
                p.setColor(Color.LTGRAY);
                c.drawCircle(wx, cy + h * .52f, 4 * scale, p);
                p.setColor(Color.DKGRAY);
            }

            // Motor block and a safe top module (scanner) instead of a weapon.
            p.setColor(player && motor < 35 ? Color.rgb(100, 60, 50) : Color.rgb(58, 72, 58));
            c.drawRect(cx - 18, cy - 2, cx + 15, cy + 14, p);
            p.setColor(player ? Color.rgb(55, 87, 58) : Color.rgb(70, 71, 73));
            c.drawCircle(cx, cy - 8 * scale, 17 * scale, p);
            p.setColor(Color.rgb(190, 210, 220));
            c.drawRect(cx - 3, cy - 31 * scale, cx + 3, cy - 11 * scale, p);
            c.drawCircle(cx, cy - 33 * scale, 5 * scale, p);

            if (player && (engineBroken() || wheelsBroken())) {
                p.setColor(Color.argb(150, 70, 70, 70));
                c.drawCircle(cx + 23 * scale, cy - 26 * scale, 8 * scale, p);
            }
            c.restore();
        }

        boolean engineBroken() { return motor <= 0; }
        boolean wheelsBroken() { return wheels <= 0; }

        void drawRival(Canvas c, Rival r) {
            float X = r.x - cam;
            float Y = ground(r.x) - 43 * r.size;
            if (X < -150 || X > getWidth() + 150) return;
            drawVehicle(c, X, Y, 1, r.size, 0, false);
            p.setColor(Color.DKGRAY);
            c.drawRect(X - 48 * r.size, Y - 66 * r.size, X + 48 * r.size, Y - 59 * r.size, p);
            p.setColor(Color.rgb(255, 190, 70));
            c.drawRect(X - 48 * r.size, Y - 66 * r.size,
                    X - 48 * r.size + 96 * r.size * (r.durability / r.maxDurability),
                    Y - 59 * r.size, p);
            if (r.size > 1.3f) txt(c, "TO", X, Y - 75 * r.size, 12, Color.WHITE);
        }

        void drawPart(Canvas c, Part part) {
            float X = part.x - cam;
            if (X < -40 || X > getWidth() + 40) return;
            p.setColor(Color.rgb(70, 75, 80));
            if (part.type == Part.WHEEL) {
                c.drawCircle(X, part.y, 11, p);
                p.setColor(Color.LTGRAY);
                c.drawCircle(X, part.y, 4, p);
            } else if (part.type == Part.ARMOR) {
                c.drawRoundRect(new RectF(X - 18, part.y - 6, X + 18, part.y + 6), 4, 4, p);
            } else {
                c.drawRoundRect(new RectF(X - 13, part.y - 10, X + 13, part.y + 10), 4, 4, p);
            }
        }

        void drawHud(Canvas c) {
            p.setColor(Color.argb(205, 20, 25, 28));
            c.drawRoundRect(new RectF(10, 10, getWidth() - 10, 94), 12, 12, p);
            txtL(c, "MÀN " + level + "/15", 22, 34, 16, Color.WHITE);
            txtL(c, "HP " + Math.max(0, hp) + "/" + HP[selected], 115, 34, 16, Color.WHITE);
            txtL(c, "XU " + coins, getWidth() - 150, 34, 16, Color.WHITE);

            drawBar(c, 115, 52, 70, chassis, Color.rgb(255, 95, 95));
            drawBar(c, 205, 52, 70, armor, Color.rgb(240, 210, 70));
            drawBar(c, 295, 52, 70, wheels, Color.rgb(120, 200, 255));
            drawBar(c, 385, 52, 70, motor, Color.rgb(120, 235, 120));
            txtL(c, "KHUNG", 115, 76, 10, Color.WHITE);
            txtL(c, "GIÁP", 205, 76, 10, Color.WHITE);
            txtL(c, "BÁNH", 295, 76, 10, Color.WHITE);
            txtL(c, "MOTOR", 385, 76, 10, Color.WHITE);
            txtR(c, "TỰ SỬA 4.5s", getWidth() - 20, 76, 10, Color.WHITE);
        }

        void drawBar(Canvas c, float x, float y, float w, int value, int fillColor) {
            p.setColor(Color.DKGRAY);
            c.drawRect(x, y, x + w, y + 7, p);
            p.setColor(fillColor);
            c.drawRect(x, y, x + w * (Math.max(0, Math.min(100, value)) / 100f), y + 7, p);
        }

        void drawControls(Canvas c) {
            float cy = getHeight() - 88;
            p.setColor(Color.argb(110, 0, 0, 0));
            c.drawCircle(105, cy, 66, p);
            c.drawCircle(getWidth() - 88, cy, 55, p);
            c.drawCircle(getWidth() - 205, cy, 48, p);
            p.setColor(Color.argb(185, 235, 235, 235));
            c.drawCircle(knobX, knobY, 24, p);
            txt(c, "← →", 105, cy + 6, 18, Color.WHITE);
            txt(c, "SỬA", getWidth() - 88, cy + 6, 14, Color.WHITE);
            txt(c, "MÁY", getWidth() - 205, cy + 6, 14, Color.WHITE);
        }

        void drawMenu(Canvas c) {
            c.drawColor(Color.rgb(75, 155, 215));
            txt(c, win ? "THẮNG!" : lose ? "XE HỎNG" : shop ? "CỬA HÀNG XE" : "TANK ADVENTURES",
                    getWidth() / 2, 70, 40, Color.WHITE);

            if (win || lose) {
                txt(c, "+100 xu khi thắng • cần 2 lần thắng để mở màn tiếp", getWidth() / 2, 110, 15, Color.WHITE);
                btn(c, getWidth() / 2 - 145, 170, getWidth() / 2 + 145, 225, "CHƠI LẠI");
                return;
            }

            if (shop) {
                drawShop(c);
                return;
            }

            txt(c, "Lắp ghép: KHUNG + GIÁP + BÁNH + MOTOR", getWidth() / 2, 105, 17, Color.WHITE);
            txt(c, "Va chạm làm hỏng từng bộ phận • tự sửa • có thể rơi phụ tùng", getWidth() / 2, 132, 14, Color.WHITE);
            btn(c, getWidth() / 2 - 160, 165, getWidth() / 2 + 160, 220, "CHƠI MÀN " + level);
            btn(c, getWidth() / 2 - 160, 235, getWidth() / 2 + 160, 290, "CỬA HÀNG");
        }

        void drawShop(Canvas c) {
            int y = 135;
            for (int i = 0; i < 4; i++) {
                boolean canBuy = i <= owned;
                String name = i == 0 ? "SCOUT" : i == 1 ? "PANZER" : i == 2 ? "TIGER" : "TITAN";
                String state = i == selected ? "ĐANG DÙNG" : canBuy ? (i <= owned ? "CHỌN" : "KHÓA") : ("MUA " + COST[i]);
                p.setColor(Color.argb(195, 20, 25, 28));
                c.drawRoundRect(new RectF(50, y, getWidth() - 50, y + 62), 10, 10, p);
                txtL(c, name, 70, y + 27, 17, Color.WHITE);
                txtL(c, "HP " + HP[i] + "  •  tốc độ " + SPEED[i], 170, y + 27, 14, Color.WHITE);
                txtR(c, state, getWidth() - 70, y + 27, 14, Color.YELLOW);
                y += 70;
            }
            txt(c, "Xu: " + coins + "   •   Chạm vào xe để mua/chọn", getWidth() / 2, getHeight() - 20, 15, Color.WHITE);
        }

        void handleShopTouch(float ty) {
            int index = (int)((ty - 135) / 70);
            if (index < 0 || index > 3) return;
            if (index <= owned) {
                selected = index;
                save();
            } else if (index == owned + 1 && coins >= COST[index]) {
                coins -= COST[index];
                owned = index;
                selected = index;
                save();
            }
            shop = true;
            invalidate();
        }

        void btn(Canvas c, float l, float t, float rr, float b, String text) {
            p.setColor(Color.argb(210, 20, 25, 28));
            c.drawRoundRect(new RectF(l, t, rr, b), 12, 12, p);
            txt(c, text, (l + rr) / 2, (t + b) / 2 + 6, 17, Color.WHITE);
        }

        void txt(Canvas c, String s, float x, float y, float size, int color) {
            p.setColor(color);
            p.setTextSize(size);
            p.setTextAlign(Paint.Align.CENTER);
            c.drawText(s, x, y, p);
        }

        void txtL(Canvas c, String s, float x, float y, float size, int color) {
            p.setColor(color);
            p.setTextSize(size);
            p.setTextAlign(Paint.Align.LEFT);
            c.drawText(s, x, y, p);
        }

        void txtR(Canvas c, String s, float x, float y, float size, int color) {
            p.setColor(color);
            p.setTextSize(size);
            p.setTextAlign(Paint.Align.RIGHT);
            c.drawText(s, x, y, p);
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
            float tx = e.getX(), ty = e.getY();
            if (e.getAction() == MotionEvent.ACTION_DOWN) {
                if (playing) {
                    float cy = getHeight() - 88;
                    if (tx < getWidth() / 2 && ty > getHeight() - 180) {
                        joystickX = 105;
                        joystickY = cy;
                        knobX = Math.max(45, Math.min(165, tx));
                        knobY = cy;
                    } else if (tx > getWidth() - 150 && ty > getHeight() - 165) {
                        repairAll();
                    }
                    return true;
                }
                if (win || lose) {
                    if (ty > 140) startLevel();
                    return true;
                }
                if (shop) {
                    handleShopTouch(ty);
                    if (ty > getHeight() - 70) shop = false;
                    return true;
                }
                if (ty > 155 && ty < 230) startLevel();
                else if (ty > 235 && ty < 300) shop = true;
                return true;
            }

            if (playing && (e.getAction() == MotionEvent.ACTION_MOVE || e.getAction() == MotionEvent.ACTION_UP)) {
                if (tx < getWidth() / 2 && ty > getHeight() - 210) {
                    knobX = Math.max(45, Math.min(165, tx));
                }
                if (e.getAction() == MotionEvent.ACTION_UP) {
                    knobX = joystickX;
                }
                return true;
            }
            return true;
        }

        static class Rival {
            float x, speed, size, durability, maxDurability;
            long lastHit;
            boolean removed;
        }

        static class Part {
            static final int ARMOR = 1, WHEEL = 2, MOTOR = 3;
            int type;
            float x, y, vx, vy;
            boolean sleep;
        }
    }
}
