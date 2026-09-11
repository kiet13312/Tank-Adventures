package com.alpvz.tankadventures;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Random;

/**
 * Side-scrolling arcade vehicle demo inspired by hill-climb mobile gameplay.
 * Rolling terrain, vehicle tilt, springy wheels, camera follow, pickups,
 * hazards, shop and 15 campaign levels are all rendered with Canvas.
 */
public class MainActivity extends Activity {
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(new GameView(this));
    }

    static final class GameView extends View {
        static final int LEVELS = 15;
        static final long LEVEL_TIME = 60000L;
        static final float GRAVITY = 0.48f;

        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Path path = new Path();
        final Random rnd = new Random(7);
        final SharedPreferences prefs;

        final int[] HP = {500, 800, 1100, 1500};
        final float[] SPEED = {4.4f, 4.8f, 5.2f, 5.7f};
        final int[] COST = {0, 1000, 2000, 3000};
        final String[] NAME = {"Scout", "Panzer", "Tiger", "Titan"};

        final ArrayList<Pickup> pickups = new ArrayList<>();
        final ArrayList<Rock> rocks = new ArrayList<>();

        int coins, unlockedLevel, wins, ownedTank, selectedTank;
        int level = 1, hp, runCoins;
        float worldX, y, vx, vy, angle, wheelSpin, bounce, cameraX;
        float joyBaseX = 105, joyBaseY, joyKnobX = 105, joyKnobY;
        long startTime, lastMs;
        boolean playing, resultWin, resultLose, shop, levels;
        boolean leftPressed, rightPressed, boostPressed;
        float touchDownX, touchDownY;

        GameView(Context c) {
            super(c);
            prefs = c.getSharedPreferences("tank_adventures", 0);
            coins = Math.max(0, prefs.getInt("coins", 0));
            unlockedLevel = clampI(prefs.getInt("unlocked", 1), 1, LEVELS);
            wins = clampI(prefs.getInt("wins", 0), 0, 1);
            ownedTank = clampI(prefs.getInt("owned", 0), 0, 3);
            selectedTank = clampI(prefs.getInt("selected", 0), 0, ownedTank);
            p.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        }

        void save() {
            prefs.edit().putInt("coins", coins).putInt("unlocked", unlockedLevel)
                    .putInt("wins", wins).putInt("owned", ownedTank)
                    .putInt("selected", selectedTank).apply();
        }

        @Override protected void onDraw(Canvas c) {
            int w = getWidth(), h = getHeight();
            if (playing) {
                update();
                drawGame(c, w, h);
            } else if (resultWin || resultLose) {
                drawGame(c, w, h);
                drawResult(c, w, h);
            } else {
                drawMenuBackground(c, w, h);
                if (shop) drawShop(c, w, h);
                else if (levels) drawLevels(c, w, h);
                else drawMenu(c, w, h);
            }
            postInvalidateDelayed(16);
        }

        void drawMenuBackground(Canvas c, int w, int h) {
            c.drawColor(Color.rgb(85, 170, 224));
            p.setColor(Color.rgb(135, 205, 244));
            c.drawCircle(w * .17f, h * .19f, 75, p);
            c.drawCircle(w * .78f, h * .16f, 105, p);
            p.setColor(Color.rgb(88, 147, 62));
            path.reset(); path.moveTo(0, h);
            for (int x = 0; x <= w; x += 10) path.lineTo(x, terrainY(x + 300, h) + 10);
            path.lineTo(w, h); path.close(); c.drawPath(path, p);
        }

        void drawMenu(Canvas c, int w, int h) {
            txt(c, "TANK ADVENTURES", w/2f, 58, 40, Color.WHITE, true);
            txt(c, "Địa hình đồi • nghiêng theo dốc • camera bám xe", w/2f, 91, 17, Color.WHITE, true);
            txt(c, "Xu: " + coins + "    Xe: " + NAME[selectedTank], w/2f, 128, 19, Color.WHITE, true);
            btn(c,w/2f-170,160,w/2f+170,215,"CHƠI MÀN "+level);
            btn(c,w/2f-170,227,w/2f+170,282,"CHỌN MÀN");
            btn(c,w/2f-170,294,w/2f+170,349,"CỬA HÀNG XE");
            tank(c,w/2f,h-125,selectedTank,1.8f,0);
            txt(c,"Thu thập xu • né đá • sống sót 60 giây",w/2f,h-22,15,Color.WHITE,true);
        }

        void drawShop(Canvas c, int w, int h) {
            txt(c,"CỬA HÀNG XE",w/2f,38,30,Color.WHITE,true);
            txt(c,"Xu: "+coins,w/2f,67,18,Color.WHITE,true);
            for(int i=0;i<4;i++){
                float x=100+i*180;
                tank(c,x,150,i,1,0);
                txt(c,NAME[i],x,218,18,Color.WHITE,true);
                txt(c,"Độ bền "+HP[i]+" • Tốc "+SPEED[i],x,242,13,Color.WHITE,true);
                String s=ownedTank>=i?(selectedTank==i?"ĐANG DÙNG":"DÙNG"):"MUA "+COST[i];
                btn(c,x-70,262,x+70,312,s);
            }
            btn(c,20,h-52,145,h-12,"QUAY LẠI");
        }

        void drawLevels(Canvas c,int w,int h){
            txt(c,"CHỌN MÀN",w/2f,38,30,Color.WHITE,true);
            for(int i=1;i<=LEVELS;i++){
                int q=i-1,col=q%5,row=q/5;float x=95+col*158,y0=78+row*72;
                btn(c,x-48,y0,x+48,y0+48,i<=unlockedLevel?""+i:"KHÓA");
            }
            txt(c,"Thắng một màn 2 lần để mở màn kế tiếp.",w/2f,h-62,15,Color.WHITE,true);
            btn(c,20,h-50,145,h-12,"QUAY LẠI");
        }

        void startLevel(int lv){
            level=clampI(lv,1,LEVELS); playing=true; resultWin=false; resultLose=false; shop=false; levels=false;
            hp=HP[selectedTank];runCoins=0;worldX=90;y=0;vx=0;vy=0;angle=0;cameraX=0;wheelSpin=0;bounce=0;
            pickups.clear();rocks.clear();buildLevelObjects();startTime=System.currentTimeMillis();lastMs=startTime;
        }

        void buildLevelObjects(){
            rnd.setSeed(level*99173L+selectedTank*37L);float x=480;
            for(int i=0;i<45;i++){
                x+=170+rnd.nextInt(330);
                Pickup q=new Pickup();q.x=x;q.yOffset=-30-rnd.nextInt(80);q.type=(i%9==0)?1:0;pickups.add(q);
                if(i%3==1){Rock r=new Rock();r.x=x+90;r.radius=14+rnd.nextInt(12);rocks.add(r);}
            }
        }

        void update(){
            long now=System.currentTimeMillis();
            float dt=Math.min(2.2f,Math.max(.25f,(now-lastMs)/16.6667f));lastMs=now;
            if(hp<=0){endLose();return;} if(now-startTime>=LEVEL_TIME){endWin();return;}
            float steer=0; if(leftPressed)steer-=1;if(rightPressed)steer+=1;
            if(Math.abs(joyKnobX-joyBaseX)>8)steer=clamp((joyKnobX-joyBaseX)/58f,-1,1);
            float accel=.085f*(1+selectedTank*.08f);
            if(steer>.08f)vx+=accel*dt; else if(steer<-.08f)vx-=accel*dt; else vx*=Math.pow(.986,dt);
            if(boostPressed)vx+=.045f*dt;
            float max=SPEED[selectedTank]*(boostPressed?1.23f:1);
            if(vx>max)vx=max;if(vx<-max*.45f)vx=-max*.45f;
            worldX+=vx*dt*1.65f;if(worldX<80)worldX=80;

            float ground=terrainY(worldX,getHeight()),targetY=ground-42;
            vy+=GRAVITY*dt;y+=vy*dt;
            if(y>=targetY){float impact=Math.abs(vy);if(impact>8){hp-=Math.min(70,(int)(impact*4.2f));bounce=Math.min(1,impact/15);}y=targetY;vy=-Math.min(5,impact*.22f);}
            float slope=terrainY(worldX+25,getHeight())-terrainY(worldX-25,getHeight());angle=(float)Math.atan2(slope,50);
            wheelSpin+=vx*dt*.15f;bounce*=.92f;
            cameraX+=((worldX-getWidth()*.32f)-cameraX)*.10f;if(cameraX<0)cameraX=0;
            collectPickups();collideRocks();
        }

        void collectPickups(){
            for(Pickup q:pickups){if(q.taken)continue;float sy=terrainY(q.x,getHeight())+q.yOffset;
                if(dist(worldX,y,q.x,sy)<58){q.taken=true;if(q.type==0){coins+=10;runCoins+=10;}else{hp=Math.min(HP[selectedTank],hp+140);runCoins+=25;}}}
        }

        void collideRocks(){
            for(Rock r:rocks)if(Math.abs(r.x-worldX)<48){float ry=terrainY(r.x,getHeight())-r.radius+3;
                if(dist(worldX,y+22,r.x,ry)<r.radius+28&&vx>.8f){hp-=18;vx*=.60f;vy=-4.2f;worldX-=10;}}
        }

        void endWin(){
            if(!playing)return;playing=false;resultWin=true;coins+=100+runCoins;wins++;
            if(wins>=2){wins=0;if(level<LEVELS)unlockedLevel=Math.max(unlockedLevel,level+1);}save();
        }
        void endLose(){playing=false;resultLose=true;save();}

        void drawGame(Canvas c,int w,int h){drawWorld(c,w,h);drawHUD(c,w,h);drawControls(c,w,h);}

        void drawWorld(Canvas c,int w,int h){
            c.drawColor(Color.rgb(86,171,226));
            p.setColor(Color.rgb(122,195,239));c.drawCircle(w*.18f,h*.20f,82,p);c.drawCircle(w*.77f,h*.15f,118,p);
            p.setColor(Color.rgb(126,151,84));drawHills(c,cameraX*.28f,h*.61f,h*.18f);
            p.setColor(Color.rgb(98,135,72));drawHills(c,cameraX*.52f,h*.70f,h*.13f);

            path.reset();path.moveTo(0,h);for(int sx=-20;sx<=w+20;sx+=8)path.lineTo(sx,terrainY(cameraX+sx,h));path.lineTo(w,h);path.close();
            p.setColor(Color.rgb(100,66,39));c.drawPath(path,p);
            p.setColor(Color.rgb(129,85,46));for(int i=0;i<9;i++){float sx=(i*170-cameraX*.75f)%(w+170);c.drawCircle(sx,h*.83f+(i%2)*18,9,p);}
            p.setColor(Color.rgb(112,183,55));path.reset();path.moveTo(0,terrainY(cameraX,h));
            for(int sx=0;sx<=w;sx+=8)path.lineTo(sx,terrainY(cameraX+sx,h)-3);path.lineTo(w,terrainY(cameraX+w,h));path.lineTo(0,terrainY(cameraX,h)-3);path.close();c.drawPath(path,p);
            for(Pickup q:pickups)if(!q.taken)drawPickup(c,q,h);for(Rock r:rocks)drawRock(c,r,h);
            tank(c,worldX-cameraX,y-bounce*7,selectedTank,1.18f,angle);
        }

        void drawHills(Canvas c,float shift,float base,float amp){
            path.reset();path.moveTo(0,getHeight());for(int x=0;x<=getWidth();x+=10){float yy=base+(float)Math.sin((x+shift)*.008)*amp+(float)Math.sin((x+shift)*.017)*amp*.35f;path.lineTo(x,yy);}path.lineTo(getWidth(),getHeight());path.close();c.drawPath(path,p);
        }

        float terrainY(float wx,int h){float base=h*.70f;return base+(float)Math.sin(wx*.0046)*70+(float)Math.sin(wx*.0108+level)*30+(float)Math.sin(wx*.022+level*.7)*13;}

        void drawPickup(Canvas c,Pickup q,int h){float x=q.x-cameraX,y0=terrainY(q.x,h)+q.yOffset;if(x<-50||x>getWidth()+50)return;
            if(q.type==0){p.setColor(Color.rgb(255,205,40));c.drawCircle(x,y0,13,p);p.setColor(Color.rgb(255,244,155));c.drawCircle(x-3,y0-3,4,p);txt(c,"$",x,y0+7,17,Color.rgb(120,80,10),true);}
            else{p.setColor(Color.rgb(85,216,105));c.drawCircle(x,y0,17,p);txt(c,"+",x,y0+8,21,Color.WHITE,true);}}

        void drawRock(Canvas c,Rock r,int h){float x=r.x-cameraX;if(x<-60||x>getWidth()+60)return;float y0=terrainY(r.x,h)-r.radius+4;p.setColor(Color.rgb(78,73,63));c.drawCircle(x,y0,r.radius,p);p.setColor(Color.rgb(110,104,92));c.drawCircle(x-5,y0-5,r.radius*.45f,p);}

        void drawHUD(Canvas c,int w,int h){
            p.setColor(Color.argb(190,20,27,32));c.drawRoundRect(new RectF(10,10,w-10,60),14,14,p);
            txtL(c,"MÀN "+level+"/15",24,39,17,Color.WHITE);txtL(c,"HP "+hp+"/"+HP[selectedTank],115,39,16,Color.WHITE);txtL(c,"XU "+coins,255,39,16,Color.WHITE);
            long left=Math.max(0,(LEVEL_TIME-(System.currentTimeMillis()-startTime)+999)/1000);txtR(c,left+"s",w-24,39,17,Color.WHITE);
            p.setColor(Color.DKGRAY);c.drawRoundRect(new RectF(115,46,230,53),4,4,p);p.setColor(Color.GREEN);c.drawRoundRect(new RectF(115,46,115+115*Math.max(0,Math.min(1,hp/(float)HP[selectedTank])),53),4,4,p);
        }

        void drawControls(Canvas c,int w,int h){float by=h-94;p.setColor(Color.argb(105,0,0,0));c.drawCircle(105,by,68,p);c.drawCircle(w-95,by,58,p);p.setColor(Color.argb(185,220,220,220));c.drawCircle(joyKnobX,joyKnobY,26,p);txt(c,"GA",w-95,by+7,18,Color.WHITE,true);}

        void drawResult(Canvas c,int w,int h){
            p.setColor(Color.argb(210,0,0,0));c.drawRect(0,0,w,h,p);
            if(resultWin){txt(c,level==LEVELS?"HOÀN THÀNH 15 MÀN":"VƯỢT MÀN!",w/2f,h/2f-55,38,Color.GREEN,true);txt(c,"+"+(100+runCoins)+" xu • thắng "+wins+"/2",w/2f,h/2f-17,20,Color.WHITE,true);txt(c,level==LEVELS?"Bạn đã hoàn tất chiến dịch":"Màn kế tiếp: "+Math.min(LEVELS,level+1),w/2f,h/2f+18,17,Color.WHITE,true);btn(c,w/2f-135,h/2f+55,w/2f+135,h/2f+108,level==LEVELS?"VỀ MENU":"CHƠI LẠI");}
            else{txt(c,"XE BỊ HƯ!",w/2f,h/2f-25,40,Color.rgb(255,90,70),true);btn(c,w/2f-135,h/2f+45,w/2f+135,h/2f+100,"CHƠI LẠI");}
        }

        @Override public boolean onTouchEvent(MotionEvent e){
            float x=e.getX(),yy=e.getY();int act=e.getActionMasked(),w=getWidth(),h=getHeight();
            if(act==MotionEvent.ACTION_DOWN){touchDownX=x;touchDownY=yy;
                if(playing){if(yy>h-190&&x<w*.58f){joyBaseX=105;joyBaseY=h-94;joyKnobX=x;joyKnobY=yy;leftPressed=x<joyBaseX-8;rightPressed=x>joyBaseX+8;}else if(yy>h-175&&x>=w*.58f)boostPressed=true;}
                else if(resultWin||resultLose){if(inside(x,yy,w/2f-150,h/2f+35,w/2f+150,h/2f+125)){if(resultWin&&level==LEVELS){startMenu();}else{startLevel(level);}}}
                else if(shop)shopTap(x,yy);else if(levels)levelTap(x,yy);else menuTap(x,yy);return true;}
            if(playing&&(act==MotionEvent.ACTION_MOVE||act==MotionEvent.ACTION_UP)){if(touchDownY>h-190&&touchDownX<w*.58f){float dx=clamp(x-joyBaseX,-58,58),dy=clamp(yy-joyBaseY,-58,58);joyKnobX=joyBaseX+dx;joyKnobY=joyBaseY+dy;leftPressed=dx<-8;rightPressed=dx>8;}if(act==MotionEvent.ACTION_UP){leftPressed=false;rightPressed=false;boostPressed=false;joyKnobX=joyBaseX;joyKnobY=joyBaseY;}return true;}return true;
        }

        void menuTap(float x,float y0){int w=getWidth();if(inside(x,y0,w/2f-170,160,w/2f+170,215))startLevel(level);else if(inside(x,y0,w/2f-170,227,w/2f+170,282))levels=true;else if(inside(x,y0,w/2f-170,294,w/2f+170,349))shop=true;}
        void levelTap(float x,float y0){int h=getHeight();if(inside(x,y0,20,h-50,145,h-12)){levels=false;return;}for(int i=1;i<=LEVELS;i++){int q=i-1,col=q%5,row=q/5;float bx=95+col*158,by=78+row*72;if(i<=unlockedLevel&&inside(x,y0,bx-48,by,bx+48,by+48)){level=i;levels=false;startLevel(level);return;}}}
        void shopTap(float x,float y0){int h=getHeight();if(inside(x,y0,20,h-52,145,h-12)){shop=false;return;}for(int i=0;i<4;i++){float bx=100+i*180;if(inside(x,y0,bx-70,262,bx+70,312)){if(ownedTank>=i)selectedTank=i;else if(i==ownedTank+1&&coins>=COST[i]){coins-=COST[i];ownedTank=i;selectedTank=i;save();}}}}
        void startMenu(){playing=false;resultWin=false;resultLose=false;shop=false;levels=false;save();}

        void btn(Canvas c,float l,float t,float r,float b,String s){p.setColor(Color.argb(220,25,34,39));c.drawRoundRect(new RectF(l,t,r,b),14,14,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(Color.argb(170,255,255,255));c.drawRoundRect(new RectF(l,t,r,b),14,14,p);p.setStyle(Paint.Style.FILL);txt(c,s,(l+r)/2,(t+b)/2+7,17,Color.WHITE,true);}
        void txt(Canvas c,String s,float x,float y,float size,int color,boolean center){p.setTextSize(size);p.setColor(color);p.setStyle(Paint.Style.FILL);p.setTextAlign(center?Paint.Align.CENTER:Paint.Align.LEFT);c.drawText(s,x,y,p);}
        void txtL(Canvas c,String s,float x,float y,float size,int color){txt(c,s,x,y,size,color,false);}
        void txtR(Canvas c,String s,float x,float y,float size,int color){p.setTextAlign(Paint.Align.RIGHT);p.setTextSize(size);p.setColor(color);c.drawText(s,x,y,p);}
        boolean inside(float x,float y,float l,float t,float r,float b){return x>=l&&x<=r&&y>=t&&y<=b;}
        float clamp(float v,float a,float b){return Math.max(a,Math.min(b,v));}
        int clampI(int v,int a,int b){return Math.max(a,Math.min(b,v));}
        float dist(float ax,float ay,float bx,float by){return (float)Math.hypot(ax-bx,ay-by);}

        void tank(Canvas c,float x,float y,int type,float s,float rot){
            c.save();c.rotate((float)Math.toDegrees(rot),x,y);int[] body={Color.rgb(45,150,67),Color.rgb(38,125,60),Color.rgb(30,104,53),Color.rgb(25,83,45)};
            float bw=78*s,bh=38*s;p.setColor(Color.argb(90,0,0,0));c.drawOval(new RectF(x-bw*.58f,y+bh*.34f,x+bw*.58f,y+bh*.56f),p);
            p.setColor(Color.rgb(35,38,38));c.drawRoundRect(new RectF(x-bw*.60f,y-bh*.15f,x+bw*.60f,y+bh*.55f),13*s,13*s,p);
            for(int i=0;i<5;i++){p.setColor(Color.rgb(85,86,83));c.drawCircle(x-bw*.43f+i*bw*.21f,y+bh*.25f,8*s,p);}
            p.setColor(body[type]);c.drawRoundRect(new RectF(x-bw*.50f,y-bh*.50f,x+bw*.50f,y+bh*.22f),10*s,10*s,p);
            p.setColor(Color.rgb(60,88,68));c.drawRoundRect(new RectF(x-bw*.15f,y-bh*.75f,x+bw*.32f,y-bh*.10f),10*s,10*s,p);
            p.setColor(Color.rgb(176,216,218));c.drawCircle(x+bw*.08f,y-bh*.47f,8*s,p);
            p.setColor(Color.rgb(72,72,72));c.drawRect(x-bw*.43f,y-bh*.67f,x-bw*.10f,y-bh*.51f,p);
            p.setColor(Color.rgb(215,200,104));c.drawRect(x-bw*.48f,y-bh*.88f,x-bw*.25f,y-bh*.77f,p);
            p.setStrokeWidth(4*s);p.setColor(Color.rgb(48,50,46));c.drawLine(x+bw*.22f,y-bh*.55f,x+bw*.47f,y-bh*.93f,p);p.setStrokeWidth(2*s);p.setColor(Color.rgb(110,215,120));c.drawLine(x+bw*.47f,y-bh*.93f,x+bw*.47f,y-bh*1.12f,p);c.restore();
        }

        static final class Pickup{float x,yOffset;int type;boolean taken;}
        static final class Rock{float x,radius;}
    }
}
