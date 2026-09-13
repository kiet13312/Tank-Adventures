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

public class TankGameView extends View {
    private static final int VW=1280, VH=720, LEVELS=15, KV2_PRICE=500;
    private static final long FIRE_DELAY=2000L;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint img=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final SharedPreferences prefs;
    private final ArrayList<Shot> shots=new ArrayList<>();
    private Bitmap ms1Body,ms1Head,kv2Body,kv2Head;
    private TankType selected=TankType.MS1;
    private boolean kv2Owned,buildMode=true,left,right,up,down,won,lost;
    private int level,coins,hp,enemyHp,enemyMax;
    private float tankX=450f,enemyX,headAngle;
    private long lastFrame,lastShot,enemyShotAt;

    public TankGameView(Context c){
        super(c); setLayerType(View.LAYER_TYPE_SOFTWARE,null);
        prefs=c.getSharedPreferences("tank_adventures_clean",Context.MODE_PRIVATE);
        level=clamp(prefs.getInt("level",1),1,LEVELS);
        coins=Math.max(0,prefs.getInt("coins",500));
        kv2Owned=prefs.getBoolean("kv2_owned",false);
        loadImages(); setDefaultHeadAngle(); resetBattle(); lastFrame=System.currentTimeMillis();
    }
    private void loadImages(){ms1Body=load("ms1body");ms1Head=load("ms1head");kv2Body=load("kv2body");kv2Head=load("kv2head");}
    private Bitmap load(String n){int id=getResources().getIdentifier(n,"drawable",getContext().getPackageName());return id==0?null:BitmapFactory.decodeResource(getResources(),id);}
    private Bitmap body(){return selected==TankType.MS1?ms1Body:kv2Body;}
    private Bitmap head(){return selected==TankType.MS1?ms1Head:kv2Head;}
    private float bodyWidth(){return selected==TankType.MS1?335f:390f;}
    private float headWidth(){return selected==TankType.MS1?190f:215f;}
    private float bodyPivotX(){return selected==TankType.MS1?.67f:.66f;}
    private float bodyPivotY(){return selected==TankType.MS1?.22f:.21f;}
    private float headAnchorX(){return selected==TankType.MS1?.48f:.46f;}
    private float headAnchorY(){return selected==TankType.MS1?.76f:.74f;}
    private void setDefaultHeadAngle(){headAngle=selected==TankType.MS1?10f:12f;}
    private void resetBattle(){shots.clear();tankX=450f;enemyX=2600f+level*120f;hp=selected==TankType.MS1?380:620;enemyMax=260+level*55;enemyHp=enemyMax;won=false;lost=false;lastShot=0;enemyShotAt=System.currentTimeMillis()+1300;}
    private void save(){prefs.edit().putInt("level",level).putInt("coins",coins).putBoolean("kv2_owned",kv2Owned).apply();}

    @Override protected void onDraw(Canvas c){
        long now=System.currentTimeMillis(); float dt=Math.min(.033f,Math.max(0,(now-lastFrame)/1000f)); lastFrame=now;
        float s=Math.min(getWidth()/(float)VW,getHeight()/(float)VH),ox=(getWidth()-VW*s)/2f,oy=(getHeight()-VH*s)/2f;
        c.save();c.translate(ox,oy);c.scale(s,s);
        if(buildMode)drawBuild(c);else{if(!won&&!lost)update(dt,now);drawBattle(c);}c.restore();postInvalidateDelayed(16);
    }

    private void drawBuild(Canvas c){
        background(c);text(c,"TANK ADVENTURES",35,42,34,true);text(c,"LẮP RÁP XE",35,76,22,false);text(c,"MÀN "+level+" / "+LEVELS+"    XU: "+coins,880,42,21,true);
        panel(c,35,105,840,575,Color.argb(145,20,28,34));text(c,"THÂN + ĐẦU XE",65,143,24,true);text(c,"Thân chỉ là thân xe. Độ nghiêng tự bám theo dốc.",65,174,17,false);drawAssembly(c);
        panel(c,890,100,1250,570,Color.argb(230,30,35,40));text(c,"CHỌN XE",925,140,24,true);
        choice(c,TankType.MS1,915,165,1215,245);choice(c,TankType.KV2,915,265,1215,345);
        text(c,"Bộ phận của xe đã chọn",925,390,19,true);partCard(c,"THÂN XE",body(),915,410,1045,500);partCard(c,"ĐẦU XE",head(),1070,410,1200,500);
        if(selected==TankType.KV2&&!kv2Owned)button(c,925,520,1215,565,"MUA KV-2 • 500 XU",Color.rgb(180,125,45));else button(c,925,520,1215,565,"ĐÃ CHỌN",Color.rgb(60,145,85));
        button(c,900,605,1065,685,"CHIẾN ĐẤU",Color.rgb(55,145,75));button(c,1080,605,1245,685,"ĐẶT LẠI",Color.rgb(165,75,55));
        panel(c,35,590,840,695,Color.argb(215,25,25,25));text(c,"Kéo ĐẦU XE vào đúng vòng tháp pháo để lắp.",60,625,19,true);text(c,"Trong trận: ▲▼ chỉ nâng/hạ ĐẦU XE. THÂN XE không lên/xuống.",60,660,16,false);
    }

    private void drawAssembly(Canvas c){
        Bitmap b=body(),h=head();if(b==null||h==null)return;
        float bw=bodyWidth(),bh=bw*b.getHeight()/(float)b.getWidth(),bx=250f,ground=505f,by=ground-bh;
        c.drawBitmap(b,null,new RectF(bx,by,bx+bw,ground),img);
        float pivotX=bx+bw*bodyPivotX(),pivotY=by+bh*bodyPivotY();
        drawHeadAtPivot(c,h,pivotX,pivotY);
    }

    private void choice(Canvas c,TankType t,float l,float top,float r,float bot){
        boolean sel=selected==t,locked=t==TankType.KV2&&!kv2Owned;p.setStyle(Paint.Style.FILL);p.setColor(sel?Color.rgb(70,125,80):Color.rgb(55,63,68));c.drawRoundRect(l,top,r,bot,18,18,p);
        text(c,t==TankType.MS1?"MS-1  •  MIỄN PHÍ":(locked?"KV-2  •  500 XU":"KV-2  •  ĐÃ MUA"),l+18,top+32,20,true);
        Bitmap b=t==TankType.MS1?ms1Body:kv2Body,h=t==TankType.MS1?ms1Head:kv2Head;if(b!=null)drawFit(c,b,l+12,top+42,l+150,bot-8);if(h!=null)drawFit(c,h,l+150,top+42,l+288,bot-8);
    }
    private void partCard(Canvas c,String label,Bitmap b,float l,float t,float r,float bot){panel(c,l,t,r,bot,Color.rgb(52,60,66));text(c,label,l+8,t+18,13,true);if(b!=null)drawFit(c,b,l+8,t+25,r-8,bot-8);}

    private void drawBattle(Canvas c){
        background(c);float cam=Math.max(0,Math.min(levelLength()-VW,tankX-470));c.save();c.translate(-cam,0);drawTerrain(c);drawFinish(c);drawPlayer(c);drawEnemy(c);for(Shot s:shots)s.draw(c);c.restore();
        text(c,"MÀN "+level+"/"+LEVELS,25,34,23,true);text(c,"HP "+Math.max(0,hp),25,63,21,true);text(c,"XU "+coins,25,91,19,false);text(c,selected==TankType.MS1?"MS-1":"KV-2",25,118,19,true);text(c,"ĐẦU XE "+(int)headAngle+"°",25,145,18,false);text(c,"ENEMY "+Math.max(0,enemyHp),1025,34,22,true);text(c,"MÁU BẠN / ĐỊCH",1025,61,15,false);
        long rem=Math.max(0,FIRE_DELAY-(System.currentTimeMillis()-lastShot));text(c,rem==0?"BẮN SẴN":"HỒI "+String.format(Locale.US,"%.1fs",rem/1000f),25,173,17,true);
        button(c,20,585,115,685,"◀",Color.rgb(55,80,100));button(c,125,585,220,685,"▶",Color.rgb(55,80,100));button(c,230,585,325,685,"▲",Color.rgb(125,95,55));button(c,330,585,425,685,"▼",Color.rgb(125,95,55));button(c,875,585,1060,685,"LẮP RÁP",Color.rgb(60,130,75));button(c,1080,585,1250,685,"BẮN",Color.rgb(185,55,45));
        if(won){panel(c,330,210,950,500,Color.argb(235,20,85,35));text(c,"VICTORY!",500,295,56,true);text(c,"+100 XU",540,345,30,true);if(level<LEVELS)button(c,490,390,790,465,"MÀN TIẾP",Color.rgb(55,145,75));else text(c,"HOÀN THÀNH 15 MÀN!",435,410,27,true);}
        if(lost){panel(c,330,210,950,500,Color.argb(235,95,30,30));text(c,"DEFEAT",525,295,56,true);button(c,490,390,790,465,"CHƠI LẠI",Color.rgb(175,75,55));}
    }

    private void drawPlayer(Canvas c){
        Bitmap b=body(),h=head();if(b==null||h==null)return;
        float ground=terrainY(tankX),bw=bodyWidth(),bh=bw*b.getHeight()/(float)b.getWidth(),leftX=tankX-bw*.5f,top=ground-bh;
        float slope=(float)Math.toDegrees(Math.atan(terrainSlope(tankX)));
        c.save();c.rotate(slope,tankX,ground);c.drawBitmap(b,null,new RectF(leftX,top,leftX+bw,ground),img);c.restore();
        float pivotX=leftX+bw*bodyPivotX(),pivotY=top+bh*bodyPivotY();
        drawHeadAtPivot(c,h,pivotX,pivotY);
    }

    private void drawHeadAtPivot(Canvas c,Bitmap h,float pivotX,float pivotY){
        float hw=headWidth(),hh=hw*h.getHeight()/(float)h.getWidth();
        float ax=hw*headAnchorX(),ay=hh*headAnchorY();
        c.save();c.rotate(-headAngle,pivotX,pivotY);c.drawBitmap(h,null,new RectF(pivotX-ax,pivotY-ay,pivotX-ax+hw,pivotY-ay+hh),img);c.restore();
    }

    private void drawEnemy(Canvas c){float y=terrainY(enemyX);p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(145,55,55));c.drawRect(enemyX-100,y-110,enemyX+100,y-25,p);p.setColor(Color.rgb(190,75,75));c.drawCircle(enemyX,y-130,65,p);p.setColor(Color.DKGRAY);c.drawRect(enemyX+30,y-145,enemyX+135,y-125,p);bar(c,enemyX-100,y-185,enemyX+100,y-170,enemyHp/(float)Math.max(1,enemyMax));}

    private void update(float dt,long now){
        if(left)tankX-=260*dt;if(right)tankX+=260*dt;tankX=Math.max(170,Math.min(levelLength()-170,tankX));
        if(up)headAngle=Math.max(-35,headAngle-75*dt);if(down)headAngle=Math.min(55,headAngle+75*dt);
        if(enemyX>tankX+280)enemyX-=Math.min(45*dt,enemyX-(tankX+280));else if(enemyX<tankX+280)enemyX+=Math.min(25*dt,tankX+280-enemyX);
        if(now>=enemyShotAt){enemyShotAt=now+2600;shots.add(new Shot(enemyX-80,terrainY(enemyX)-125,tankX,terrainY(tankX)-100,false,18));}
        Iterator<Shot> it=shots.iterator();while(it.hasNext()){Shot s=it.next();s.update(dt);if(s.hit(this)){if(s.player)enemyHp-=s.damage;else hp-=s.damage;it.remove();}else if(s.off())it.remove();}
        if(enemyHp<=0&&!won){won=true;coins+=100;save();}if(hp<=0)lost=true;
    }

    private void fire(){long now=System.currentTimeMillis();if(now-lastShot<FIRE_DELAY||won||lost)return;lastShot=now;Bitmap b=body();float bw=bodyWidth(),bh=bw*b.getHeight()/(float)b.getWidth(),ground=terrainY(tankX),top=ground-bh,pivotX=tankX-bw*.5f+bw*bodyPivotX(),pivotY=top+bh*bodyPivotY(),a=(float)Math.toRadians(-headAngle),m=headWidth()*.88f,sx=pivotX+(float)Math.cos(a)*m,sy=pivotY+(float)Math.sin(a)*m;shots.add(new Shot(sx,sy,sx+2200,sy+(float)Math.sin(a)*300,true,35));}
    private void nextLevel(){if(level>=LEVELS)return;level++;save();buildMode=true;setDefaultHeadAngle();resetBattle();}
    private void resetAll(){level=1;coins=500;kv2Owned=false;selected=TankType.MS1;setDefaultHeadAngle();save();resetBattle();}

    private void drawTerrain(Canvas c){Path q=new Path();q.moveTo(0,terrainY(0)+100);for(int x=0;x<=levelLength();x+=20)q.lineTo(x,terrainY(x));q.lineTo(levelLength(),720);q.lineTo(0,720);q.close();p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(75,115,65));c.drawPath(q,p);p.setColor(Color.rgb(105,145,75));for(int x=0;x<levelLength();x+=180)c.drawRect(x,terrainY(x),x+120,terrainY(x)+6,p);}
    private void drawFinish(Canvas c){float x=levelLength()-140,y=terrainY(x);p.setColor(Color.WHITE);p.setStrokeWidth(8);c.drawLine(x,y-210,x,y,p);p.setStyle(Paint.Style.FILL);p.setColor(Color.YELLOW);c.drawRect(x,y-210,x+110,y-150,p);}
    private float terrainY(float x){float a=40*(float)Math.sin(x*.0031)+25*(float)Math.sin(x*.0077),r=0;if(x>650&&x<1150)r=(x-650)*.18f;else if(x>=1150&&x<1500)r=90-(x-1150)*.257f;return 530-a-r;}
    private float terrainSlope(float x){float e=2;return(terrainY(x+e)-terrainY(x-e))/(2*e);}
    private float levelLength(){return 3000+level*180;}
    private void drawFit(Canvas c,Bitmap b,float l,float t,float r,float bot){float bw=b.getWidth(),bh=b.getHeight(),s=Math.min((r-l)/bw,(bot-t)/bh),w=bw*s,h=bh*s,cx=(l+r)/2,cy=(t+bot)/2;c.drawBitmap(b,null,new RectF(cx-w/2,cy-h/2,cx+w/2,cy+h/2),img);}
    private void background(Canvas c){p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(27,38,47));c.drawRect(0,0,VW,VH,p);p.setColor(Color.rgb(50,80,105));c.drawRect(0,250,VW,520,p);p.setColor(Color.rgb(95,135,165));c.drawCircle(1100,120,75,p);}
    private void panel(Canvas c,float l,float t,float r,float b,int color){p.setStyle(Paint.Style.FILL);p.setColor(color);c.drawRoundRect(l,t,r,b,18,18,p);}
    private void button(Canvas c,float l,float t,float r,float b,String s,int color){p.setStyle(Paint.Style.FILL);p.setColor(color);c.drawRoundRect(l,t,r,b,16,16,p);text(c,s,(l+r)/2-s.length()*6.3f,t+(b-t)*.62f,20,true);}
    private void text(Canvas c,String s,float x,float y,float size,boolean bold){p.setStyle(Paint.Style.FILL);p.setColor(Color.WHITE);p.setTextSize(size);p.setTypeface(bold?Typeface.DEFAULT_BOLD:Typeface.DEFAULT);c.drawText(s,x,y,p);}
    private void bar(Canvas c,float l,float t,float r,float b,float pct){p.setStyle(Paint.Style.FILL);p.setColor(Color.DKGRAY);c.drawRoundRect(l,t,r,b,6,6,p);p.setColor(Color.rgb(70,190,85));c.drawRoundRect(l,t,l+(r-l)*Math.max(0,Math.min(1,pct)),b,6,6,p);}

    @Override public boolean onTouchEvent(MotionEvent e){
        float s=Math.min(getWidth()/(float)VW,getHeight()/(float)VH),ox=(getWidth()-VW*s)/2f,oy=(getHeight()-VH*s)/2f,x=(e.getX()-ox)/s,y=(e.getY()-oy)/s;int a=e.getActionMasked();
        if(a==MotionEvent.ACTION_DOWN||a==MotionEvent.ACTION_MOVE){if(!buildMode){left=x>=20&&x<115&&y>=585;right=x>=125&&x<220&&y>=585;up=x>=230&&x<325&&y>=585;down=x>=330&&x<425&&y>=585;}return true;}
        if(a==MotionEvent.ACTION_UP||a==MotionEvent.ACTION_CANCEL){
            if(!buildMode){boolean fire=x>=1080&&y>=585,asm=x>=875&&x<1060&&y>=585,next=won&&level<LEVELS&&x>=490&&x<790&&y>=390&&y<465,retry=lost&&x>=490&&x<790&&y>=390&&y<465;left=right=up=down=false;if(fire)fire();else if(asm){buildMode=true;setDefaultHeadAngle();}else if(next)nextLevel();else if(retry)resetBattle();return true;}
            if(y>=165&&y<245&&x>=915&&x<=1215){selected=TankType.MS1;setDefaultHeadAngle();resetBattle();return true;}
            if(y>=265&&y<345&&x>=915&&x<=1215){if(kv2Owned){selected=TankType.KV2;setDefaultHeadAngle();resetBattle();}return true;}
            if(selected==TankType.KV2&&!kv2Owned&&x>=925&&x<=1215&&y>=520&&y<=565&&coins>=KV2_PRICE){coins-=KV2_PRICE;kv2Owned=true;save();return true;}
            if(x>=900&&x<=1065&&y>=605){buildMode=false;resetBattle();return true;}
            if(x>=1080&&x<=1245&&y>=605){resetAll();return true;}
            return true;
        }return true;
    }
    private static int clamp(int v,int a,int b){return Math.max(a,Math.min(b,v));}
    private enum TankType{MS1,KV2}
    private static class Shot{
        float x,y,tx,ty;final boolean player;final int damage;final float speed=850;
        Shot(float x,float y,float tx,float ty,boolean player,int damage){this.x=x;this.y=y;this.tx=tx;this.ty=ty;this.player=player;this.damage=damage;}
        void update(float dt){float dx=tx-x,dy=ty-y,d=(float)Math.sqrt(dx*dx+dy*dy);if(d>1){float q=Math.min(speed*dt,d);x+=dx/d*q;y+=dy/d*q;}}
        boolean hit(TankGameView g){if(player)return Math.abs(x-g.enemyX)<115&&Math.abs(y-(g.terrainY(g.enemyX)-100))<90;return Math.abs(x-g.tankX)<170&&Math.abs(y-(g.terrainY(g.tankX)-100))<130;}
        boolean off(){return x<-500||x>4000||y<-300||y>900;}
        void draw(Canvas c){Paint q=new Paint(Paint.ANTI_ALIAS_FLAG);q.setColor(player?Color.YELLOW:Color.RED);c.drawCircle(x,y,9,q);}
    }
}
