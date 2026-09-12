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
        prefs.edit().putInt("level", level).putInt("coins", coins).putBoolean("kv2_owned", kv2Owned).apply();
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
        if (buildMode) drawBuildScreen(canvas); else { if (!levelWon && !levelLost) updateBattle(dt, now); drawBattleScreen(canvas); }
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
        if (selectedTank == TankType.KV2 && !kv2Owned) button(c, 925, 520, 1215, 565, "MUA KV-2 • 500 XU", Color.rgb(180, 125, 45)); else button(c, 925, 520, 1215, 565, "ĐÃ CHỌN", Color.rgb(60, 145, 85));
        button(c, 900, 605, 1065, 685, "CHIẾN ĐẤU", Color.rgb(55, 145, 75));
        button(c, 1080, 605, 1245, 685, "ĐẶT LẠI", Color.rgb(165, 75, 55));
        panel(c, 35, 590, 840, 695, Color.argb(215, 25, 25, 25));
        text(c, "Kéo ĐẦU XE vào trên THÂN XE để lắp.", 60, 625, 19, true);
        text(c, "Trong trận: ▲▼ chỉ nâng/hạ ĐẦU XE. THÂN XE không có điều khiển lên/xuống.", 60, 660, 16, false);
    }

    private void drawAssemblyPreview(Canvas c) {
        Bitmap body = bodyBitmap(); Bitmap head = headBitmap();
        if (body != null) c.drawBitmap(body, null, fitRect(body, 180, 360, 560, 560), imagePaint);
        if (head != null) { c.save(); c.rotate(-headAngle, 600, 345); c.drawBitmap(head, null, fitRect(head, 545, 250, 760, 410), imagePaint); c.restore(); }
    }

    private void tankChoice(Canvas c, TankType type, float l, float t, float r, float b) {
        boolean selected = selectedTank == type; boolean locked = type == TankType.KV2 && !kv2Owned;
        paint.setStyle(Paint.Style.FILL); paint.setColor(selected ? Color.rgb(70,125,80) : Color.rgb(55,63,68)); c.drawRoundRect(l,t,r,b,18,18,paint);
        String name = type == TankType.MS1 ? "MS-1  •  MIỄN PHÍ" : (locked ? "KV-2  •  500 XU" : "KV-2  •  ĐÃ MUA");
        text(c,name,l+18,t+32,20,true);
        Bitmap body = type == TankType.MS1 ? ms1Body : kv2Body; Bitmap head = type == TankType.MS1 ? ms1Head : kv2Head;
        if (body != null) c.drawBitmap(body,null,fitRect(body,l+15,t+38,l+150,b-8),imagePaint);
        if (head != null) c.drawBitmap(head,null,fitRect(head,l+150,t+38,l+285,b-8),imagePaint);
    }

    private void smallPart(Canvas c,String label,Bitmap bitmap,float l,float t,float r,float b,float angle) {
        paint.setStyle(Paint.Style.FILL); paint.setColor(Color.rgb(52,60,66)); c.drawRoundRect(l,t,r,b,14,14,paint);
        if(bitmap!=null){c.save();c.rotate(angle,(l+r)*.5f,(t+b)*.5f);c.drawBitmap(bitmap,null,fitRect(bitmap,l+8,t+20,r-8,b-12),imagePaint);c.restore();}
        text(c,label,l+8,t+18,13,true);
    }

    private void drawBattleScreen(Canvas c) {
        drawBackground(c); float camera=Math.max(0f,Math.min(levelLength()-VW,worldX-470f)); c.save(); c.translate(-camera,0); drawTerrain(c,camera); drawFinish(c); drawPlayer(c); drawEnemy(c); for(Projectile p:projectiles)p.draw(c); c.restore();
        text(c,"MÀN "+level+"/"+LEVELS,25,34,23,true); text(c,"HP "+playerHp,25,63,21,true); text(c,"XU "+coins,25,91,19,false); text(c,selectedTank==TankType.MS1?"MS-1":"KV-2",25,118,19,true); text(c,"ĐẦU XE "+(int)headAngle+"°",25,145,18,false); text(c,"ENEMY "+Math.max(0,enemyHp),1025,34,22,true); text(c,"MÁU BẠN / ĐỊCH",1025,61,15,false);
        long remain=Math.max(0L,PLAYER_FIRE_COOLDOWN-(System.currentTimeMillis()-lastPlayerShot)); text(c,remain==0?"BẮN SẴN":"HỒI "+String.format(Locale.US,"%.1fs",remain/1000f),25,173,17,true);
        button(c,20,585,115,685,"◀",Color.rgb(55,80,100)); button(c,125,585,220,685,"▶",Color.rgb(55,80,100)); button(c,230,585,325,685,"▲",Color.rgb(125,95,55)); button(c,330,585,425,685,"▼",Color.rgb(125,95,55)); button(c,875,585,1060,685,"LẮP RÁP",Color.rgb(60,130,75)); button(c,1080,585,1250,685,"BẮN",Color.rgb(185,55,45));
        if(levelWon){panel(c,330,210,950,500,Color.argb(235,20,85,35));text(c,"VICTORY!",500,295,56,true);text(c,"+100 XU",540,345,30,true);if(level<LEVELS)button(c,490,390,790,465,"MÀN TIẾP",Color.rgb(55,145,75));else text(c,"HOÀN THÀNH 15 MÀN!",435,410,27,true);}
        if(levelLost){panel(c,330,210,950,500,Color.argb(235,95,30,30));text(c,"DEFEAT",525,295,56,true);button(c,490,390,790,465,"CHƠI LẠI",Color.rgb(175,75,55));}
    }

    private void drawPlayer(Canvas c) {
        Bitmap body=bodyBitmap(),head=headBitmap(); if(body==null||head==null)return;
        float ground=terrainY(worldX),bodyWidth=330f,bodyHeight=220f,bodyTop=ground-165f,bodyCenterX=worldX,bodyCenterY=bodyTop+bodyHeight*.55f;
        float slope=(float)Math.toDegrees(Math.atan(terrainSlope(worldX)));
        c.save(); c.rotate(slope,bodyCenterX,ground); c.drawBitmap(body,null,new RectF(bodyCenterX-bodyWidth*.5f,bodyTop,bodyCenterX+bodyWidth*.5f,bodyTop+bodyHeight),imagePaint); c.restore();
        float pivotX=bodyCenterX+25f,pivotY=bodyTop+62f,headWidth=selectedTank==TankType.MS1?235f:260f,headHeight=170f;
        c.save(); c.rotate(-headAngle,pivotX,pivotY); RectF hd=new RectF(pivotX-headWidth*.5f,pivotY-headHeight*.5f,pivotX+headWidth*.5f,pivotY+headHeight*.5f); c.drawBitmap(head,null,hd,imagePaint); c.restore();
    }

    private void drawEnemy(Canvas c){
        float y=terrainY(enemyX); paint.setStyle(Paint.Style.FILL); paint.setColor(Color.rgb(145,55,55)); c.drawRect(enemyX-100,y-110,enemyX+100,y-25,paint); paint.setColor(Color.rgb(190,75,75)); c.drawCircle(enemyX,y-130,65,paint); paint.setColor(Color.DKGRAY); c.drawRect(enemyX+30,y-145,enemyX+135,y-125,paint); bar(c,enemyX-100,y-185,enemyX+100,y-170,enemyHp/(float)Math.max(1,enemyMaxHp));
    }

    private void updateBattle(float dt,long now){
        if(leftPressed)worldX-=260f*dt; if(rightPressed)worldX+=260f*dt; worldX=Math.max(170f,Math.min(levelLength()-170f,worldX));
        if(upPressed)headAngle=Math.max(-35f,headAngle-75f*dt); if(downPressed)headAngle=Math.min(55f,headAngle+75f*dt);
        if(enemyX>worldX+280f)enemyX-=Math.min(45f*dt,enemyX-(worldX+280f)); else if(enemyX<worldX+280f)enemyX+=Math.min(25f*dt,(worldX+280f)-enemyX);
        if(now>=enemyNextShot){enemyNextShot=now+2600L;projectiles.add(new Projectile(enemyX-80f,terrainY(enemyX)-125f,worldX,terrainY(worldX)-100f,false,18));}
        Iterator<Projectile> it=projectiles.iterator(); while(it.hasNext()){Projectile p=it.next(); p.update(dt); if(p.hitTarget(this)){if(p.fromPlayer)enemyHp-=p.damage;else playerHp-=p.damage;it.remove();}}
        if(enemyHp<=0){levelWon=true;coins+=100;saveProgress();} if(playerHp<=0)levelLost=true;
    }

    private void fire(){long now=System.currentTimeMillis();if(now-lastPlayerShot<PLAYER_FIRE_COOLDOWN||levelWon||levelLost)return;lastPlayerShot=now;float a=(float)Math.toRadians(-headAngle);float sx=worldX+(float)Math.cos(a)*150f;float sy=terrainY(worldX)-105f+(float)Math.sin(a)*150f;projectiles.add(new Projectile(sx,sy,worldX+2200f,sy+(float)Math.sin(a)*300f,true,35));}

    private void nextLevel(){if(level<LEVELS){level++;saveProgress();buildMode=true;resetTankParts();resetBattle();}}

    private void resetAll(){level=1;coins=0;kv2Owned=false;selectedTank=TankType.MS1;saveProgress();resetTankParts();resetBattle();}

    private void drawTerrain(Canvas c,float camera){Path p=new Path();p.moveTo(0,terrainY(0)+100);for(int x=0;x<=levelLength();x+=20)p.lineTo(x,terrainY(x));p.lineTo(levelLength(),720);p.lineTo(0,720);p.close();paint.setStyle(Paint.Style.FILL);paint.setColor(Color.rgb(75,115,65));c.drawPath(p,paint);paint.setColor(Color.rgb(105,145,75));for(int x=0;x<levelLength();x+=180)c.drawRect(x,terrainY(x),x+120,terrainY(x)+6,paint);}

    private void drawFinish(Canvas c){float x=levelLength()-140;float y=terrainY(x);paint.setColor(Color.WHITE);paint.setStrokeWidth(8);c.drawLine(x,y-210,x,y,paint);paint.setStyle(Paint.Style.FILL);paint.setColor(Color.YELLOW);c.drawRect(x,y-210,x+110,y-150,paint);}

    private float terrainY(float x){float a=40f*(float)Math.sin(x*.0031f)+25f*(float)Math.sin(x*.0077f);float ramp=0f;if(x>650&&x<1150)ramp=(x-650)*.18f;else if(x>=1150&&x<1500)ramp=90f-(x-1150)*.257f;return 530f-a-ramp;}

    private float terrainSlope(float x){float e=2f;return (terrainY(x+e)-terrainY(x-e))/(2f*e);}

    private float levelLength(){return 3000f+level*180f;}

    private RectF fitRect(Bitmap b,float l,float t,float r,float bot){float bw=b.getWidth(),bh=b.getHeight(),sx=(r-l)/bw,sy=(bot-t)/bh,s=Math.min(sx,sy),w=bw*s,h=bh*s;float cx=(l+r)*.5f,cy=(t+bot)*.5f;return new RectF(cx-w*.5f,cy-h*.5f,cx+w*.5f,cy+h*.5f);}

    private void drawBackground(Canvas c){paint.setStyle(Paint.Style.FILL);paint.setColor(Color.rgb(27,38,47));c.drawRect(0,0,VW,VH,paint);paint.setColor(Color.rgb(50,80,105));c.drawRect(0,250,VW,520,paint);paint.setColor(Color.rgb(95,135,165));c.drawCircle(1100,120,75,paint);}

    private void panel(Canvas c,float l,float t,float r,float b,int color){paint.setStyle(Paint.Style.FILL);paint.setColor(color);c.drawRoundRect(l,t,r,b,18,18,paint);}

    private void button(Canvas c,float l,float t,float r,float b,String label,int color){paint.setStyle(Paint.Style.FILL);paint.setColor(color);c.drawRoundRect(l,t,r,b,16,16,paint);text(c,label,(l+r)*.5f-(label.length()*6.3f),t+(b-t)*.62f,20,true);}

    private void text(Canvas c,String s,float x,float y,float size,boolean bold){paint.setStyle(Paint.Style.FILL);paint.setColor(Color.WHITE);paint.setTextSize(size);paint.setTypeface(bold?Typeface.DEFAULT_BOLD:Typeface.DEFAULT);c.drawText(s,x,y,paint);}

    private void bar(Canvas c,float l,float t,float r,float b,float pct){paint.setStyle(Paint.Style.FILL);paint.setColor(Color.DKGRAY);c.drawRect(l,t,r,b,paint);paint.setColor(Color.GREEN);c.drawRect(l,t,l+(r-l)*Math.max(0f,Math.min(1f,pct)),b,paint);}

    private static int clamp(int v,int lo,int hi){return Math.max(lo,Math.min(hi,v));}

    @Override public boolean onTouchEvent(MotionEvent e){float x=(e.getX()-offsetX)/scale,y=(e.getY()-offsetY)/scale;int action=e.getActionMasked();if(action==MotionEvent.ACTION_DOWN){if(buildMode){if(x>=915&&x<=1215&&y>=165&&y<=245){selectedTank=TankType.MS1;resetTankParts();}else if(x>=915&&x<=1215&&y>=265&&y<=345){selectedTank=TankType.KV2;resetTankParts();}else if(selectedTank==TankType.KV2&&!kv2Owned&&x>=925&&x<=1215&&y>=520&&y<=565&&coins>=KV2_PRICE){coins-=KV2_PRICE;kv2Owned=true;saveProgress();}else if(x>=900&&x<=1065&&y>=605&&y<=685){buildMode=false;resetBattle();}else if(x>=1080&&x<=1245&&y>=605&&y<=685){resetAll();}invalidate();return true;}else{if(x>=20&&x<=115&&y>=585&&y<=685)leftPressed=true;else if(x>=125&&x<=220&&y>=585&&y<=685)rightPressed=true;else if(x>=230&&x<=325&&y>=585&&y<=685)upPressed=true;else if(x>=330&&x<=425&&y>=585&&y<=685)downPressed=true;else if(x>=1080&&x<=1250&&y>=585&&y<=685)fire();else if(x>=875&&x<=1060&&y>=585&&y<=685){buildMode=true;resetTankParts();}else if(levelWon&&x>=490&&x<=790&&y>=390&&y<=465)nextLevel();else if(levelLost&&x>=490&&x<=790&&y>=390&&y<=465)resetBattle();return true;}}else if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL){leftPressed=rightPressed=upPressed=downPressed=false;return true;}return true;}

    private enum TankType{MS1,KV2}
    private static class TankPart{static final int BODY=0,HEAD=1;int type;float x,y;boolean welded;TankPart(int type,float x,float y){this.type=type;this.x=x;this.y=y;}}
    private static class Projectile{float x,y,tx,ty,speed=900f;boolean fromPlayer;int damage;Projectile(float x,float y,float tx,float ty,boolean fromPlayer,int damage){this.x=x;this.y=y;this.tx=tx;this.ty=ty;this.fromPlayer=fromPlayer;this.damage=damage;}void update(float dt){float dx=tx-x,dy=ty-y,d=(float)Math.sqrt(dx*dx+dy*dy);if(d>1f){x+=dx/d*speed*dt;y+=dy/d*speed*dt;}}boolean hitTarget(TankGameView g){if(fromPlayer)return Math.abs(x-g.enemyX)<65f&&Math.abs(y-(g.terrainY(g.enemyX)-130f))<90f;return Math.abs(x-g.worldX)<80f&&Math.abs(y-(g.terrainY(g.worldX)-100f))<100f;}void draw(Canvas c){paintStatic.setStyle(Paint.Style.FILL);paintStatic.setColor(fromPlayer?Color.YELLOW:Color.RED);c.drawCircle(x,y,9,paintStatic);}private static final Paint paintStatic=new Paint(Paint.ANTI_ALIAS_FLAG);}
}
