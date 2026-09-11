package com.tankadventures;

import android.content.Context;
import android.graphics.*;
import android.view.MotionEvent;
import android.view.View;
import java.util.*;

public class TankGameView extends View {
    static final int VW=1280, VH=720;
    final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    final Random rng=new Random();
    final ArrayList<Part> parts=new ArrayList<>();
    final ArrayList<Shot> shots=new ArrayList<>();
    Tank enemy;
    float scale,ox,oy,hingeAngle=18;
    boolean buildMode=true,left,right,up,down;
    int drag=-1; float ddx,ddy;
    int playerHP=430,playerArmor=170;
    long last,enemyNext;
    String msg="Kéo bộ phận vào mấu nối để HÀN";

    public TankGameView(Context c){
        super(c);setLayerType(View.LAYER_TYPE_SOFTWARE,null);
        reset();
        last=System.currentTimeMillis(); enemyNext=last+1500;
    }

    void reset(){
        parts.clear();
        parts.add(new Part(Part.FRAME,260,455));
        addWeld(Part.MOTOR,260,390,0);
        addWeld(Part.ARMOR,260,425,0);
        addWeld(Part.BIG,170,515,0);
        addWeld(Part.BIG,275,515,0);
        addWeld(Part.SMALL,225,530,0);
        addWeld(Part.HINGE,325,370,0);
        addWeld(Part.GUN,380,370,6);
        parts.get(7).angle=hingeAngle;
        enemy=new Tank(1050,475,true);
        playerHP=430;playerArmor=170;msg="Kéo bộ phận vào mấu nối để HÀN";
    }
    void addWeld(int type,float x,float y,int parent){Part a=new Part(type,x,y);a.weld=parent;parts.add(a);}

    @Override protected void onDraw(Canvas c){
        scale=Math.min(getWidth()/1280f,getHeight()/720f);
        ox=(getWidth()-VW*scale)/2f;oy=(getHeight()-VH*scale)/2f;
        c.save();c.translate(ox,oy);c.scale(scale,scale);
        bg(c);
        long now=System.currentTimeMillis();float dt=Math.min(.033f,(now-last)/1000f);last=now;
        if(buildMode)drawBuilder(c);else{updateBattle(dt,now);drawBattle(c);}
        c.restore();postInvalidateDelayed(16);
    }

    void bg(Canvas c){
        p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(67,151,196));c.drawRect(0,0,VW,VH,p);
        p.setColor(Color.rgb(103,179,83));c.drawRect(0,520,VW,VH,p);
        p.setColor(Color.rgb(174,220,111));c.drawRect(0,520,VW,548,p);
        p.setColor(Color.argb(80,255,255,255));
        c.drawCircle(170,100,35,p);c.drawCircle(215,88,48,p);c.drawCircle(263,105,32,p);
        c.drawCircle(850,125,34,p);c.drawCircle(895,115,52,p);c.drawCircle(952,130,30,p);
    }

    void drawBuilder(Canvas c){
        txt(c,"TANK ADVENTURES — LẮP RÁP",30,42,34,true);
        txt(c,"Kéo các bộ phận • mấu nối gần nhau sẽ tự HÀN",30,75,20,false);
        p.setColor(Color.argb(95,20,30,35));c.drawRoundRect(35,105,850,575,24,24,p);
        txt(c,"KHU VỰC LẮP RÁP",70,145,22,true);
        for(int i=0;i<parts.size();i++)drawPart(c,parts.get(i),i);
        p.setColor(Color.argb(220,27,32,38));c.drawRoundRect(880,95,1250,575,24,24,p);
        txt(c,"BỘ PHẬN",915,135,25,true);
        palette(c,"KHUNG",0,165);palette(c,"MOTOR",1,225);palette(c,"GIÁP",2,285);
        palette(c,"BÁNH TO",3,345);palette(c,"BÁNH NHỎ",4,405);palette(c,"BẢN LỀ",5,465);palette(c,"SÚNG",6,525);
        p.setColor(Color.argb(210,25,25,25));c.drawRoundRect(35,600,850,695,18,18,p);
        txt(c,msg,60,632,21,false);txt(c,"Mấu xanh = điểm nối. Bộ phận đã nối sẽ hiện WELDED.",60,665,18,false);
        button(c,900,605,1075,685,"CHIẾN ĐẤU",Color.rgb(55,145,70));
        button(c,1095,605,1245,685,"RESET",Color.rgb(175,75,55));
    }

    void palette(Canvas c,String s,int type,float y){
        p.setColor(Color.rgb(58,68,76));c.drawRoundRect(905,y-25,1225,y+28,14,14,p);
        txt(c,s,930,y+7,20,true);p.setColor(Color.rgb(90,235,215));c.drawCircle(1190,y,13,p);
    }

    void drawPart(Canvas c,Part a,int i){
        p.setStyle(Paint.Style.FILL);p.setColor(partColor(a.type));
        if(a.type==Part.FRAME)c.drawRoundRect(a.x-130,a.y-35,a.x+130,a.y+35,18,18,p);
        else if(a.type==Part.MOTOR)c.drawRoundRect(a.x-55,a.y-30,a.x+55,a.y+30,14,14,p);
        else if(a.type==Part.ARMOR){Path q=new Path();q.moveTo(a.x-90,a.y+28);q.lineTo(a.x-60,a.y-30);q.lineTo(a.x+75,a.y-30);q.lineTo(a.x+95,a.y+28);q.close();c.drawPath(q,p);}
        else if(a.type==Part.BIG||a.type==Part.SMALL){float r=a.type==Part.BIG?38:25;c.drawCircle(a.x,a.y,r,p);p.setColor(Color.rgb(155,155,155));c.drawCircle(a.x,a.y,r*.48f,p);}
        else if(a.type==Part.HINGE){c.drawCircle(a.x,a.y,23,p);p.setColor(Color.rgb(40,40,40));c.drawCircle(a.x,a.y,8,p);}
        else{p.setStrokeWidth(16);p.setStrokeCap(Paint.Cap.ROUND);float r=(float)Math.toRadians(a.angle);c.drawLine(a.x,a.y,a.x+150*(float)Math.cos(r),a.y-150*(float)Math.sin(r),p);}
        p.setColor(Color.rgb(90,235,215));for(PointF q:connectors(a))c.drawCircle(q.x,q.y,7,p);
        if(i==drag){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);p.setColor(Color.YELLOW);c.drawCircle(a.x,a.y,48,p);p.setStyle(Paint.Style.FILL);}
        if(a.weld>=0){txt(c,"WELDED",a.x-28,a.y+55,14,false);}
    }

    int partColor(int t){switch(t){case Part.FRAME:return Color.rgb(48,60,68);case Part.MOTOR:return Color.rgb(160,105,52);case Part.ARMOR:return Color.rgb(125,136,140);case Part.BIG:return Color.rgb(45,47,50);case Part.SMALL:return Color.rgb(63,65,69);case Part.HINGE:return Color.rgb(185,145,52);default:return Color.rgb(65,87,105);}}

    ArrayList<PointF> connectors(Part a){
        ArrayList<PointF> r=new ArrayList<>();
        if(a.type==Part.FRAME){r.add(new PointF(a.x-90,a.y));r.add(new PointF(a.x,a.y-35));r.add(new PointF(a.x+90,a.y));r.add(new PointF(a.x,a.y+35));}
        else if(a.type==Part.HINGE){r.add(new PointF(a.x,a.y));r.add(new PointF(a.x+28,a.y));}
        else r.add(new PointF(a.x,a.y));
        return r;
    }

    void drawBattle(Canvas c){
        drawPlayer(c);enemy.draw(c);for(Shot s:shots)s.draw(c);
        txt(c,"HP "+playerHP+"   GIÁP "+playerArmor,25,38,27,true);
        txt(c,"ENEMY "+enemy.hp,1060,38,27,true);
        txt(c,"NÒNG "+(int)hingeAngle+"°",25,68,19,false);
        button(c,20,585,115,685,"◀",Color.rgb(55,80,100));
        button(c,125,585,220,685,"▶",Color.rgb(55,80,100));
        button(c,230,585,325,685,"▲",Color.rgb(125,95,55));
        button(c,330,585,425,685,"▼",Color.rgb(125,95,55));
        button(c,875,585,1060,685,"LẮP RÁP",Color.rgb(60,130,75));
        button(c,1080,585,1250,685,"BẮN",Color.rgb(185,55,45));
        if(enemy.hp<=0)txt(c,"VICTORY!",510,310,60,true);
        if(playerHP<=0)txt(c,"DEFEAT",535,310,60,true);
    }

    void drawPlayer(Canvas c){
        Part f=parts.get(0);float x=f.x,y=f.y;
        p.setColor(Color.argb(65,0,0,0));c.drawOval(x-155,y+35,x+155,y+70,p);
        for(Part a:parts)if(a.type==Part.BIG||a.type==Part.SMALL){float r=a.type==Part.BIG?40:27;p.setColor(Color.rgb(43,45,48));c.drawCircle(a.x,a.y,r,p);p.setColor(Color.rgb(145,145,145));c.drawCircle(a.x,a.y,r*.48f,p);}
        p.setColor(Color.rgb(48,60,68));c.drawRoundRect(x-140,y-45,x+140,y+35,20,20,p);
        p.setColor(Color.rgb(125,136,140));c.drawRoundRect(x-95,y-65,x+80,y-20,15,15,p);
        Part h=parts.get(6),g=parts.get(7);
        p.setColor(Color.rgb(185,145,52));c.drawCircle(h.x,h.y,19,p);
        p.setColor(Color.rgb(45,45,45));p.setStrokeWidth(17);p.setStrokeCap(Paint.Cap.ROUND);
        float a=(float)Math.toRadians(g.angle);c.drawLine(h.x,h.y,h.x+155*(float)Math.cos(a),h.y-155*(float)Math.sin(a),p);
        p.setColor(Color.rgb(165,110,55));c.drawRoundRect(x-45,y-100,x+50,y-55,15,15,p);
        p.setColor(Color.argb(160,0,0,0));c.drawRect(x-120,y-145,x+120,y-132,p);p.setColor(Color.rgb(75,220,85));c.drawRect(x-120,y-145,x-120+240*Math.max(0,playerHP)/430f,y-132,p);
    }

    void updateBattle(float dt,long now){
        Part f=parts.get(0);float old=f.x;
        float speed=170 + count(Part.MOTOR)*60;
        if(left)f.x=Math.max(150,f.x-speed*dt);if(right)f.x=Math.min(760,f.x+speed*dt);
        float dx=f.x-old;for(int i=1;i<parts.size();i++)if(parts.get(i).weld>=0&&parts.get(i).weld==0)parts.get(i).x+=dx;
        Part h=parts.get(6),g=parts.get(7);
        if(up)hingeAngle=Math.min(78,hingeAngle+55*dt);if(down)hingeAngle=Math.max(5,hingeAngle-55*dt);g.angle=hingeAngle;g.x=h.x+55;g.y=h.y;
        for(Shot s:shots){if(s.dead)continue;s.vy+=760*dt;s.x+=s.vx*dt;s.y+=s.vy*dt;s.life-=dt;if(s.x<0||s.x>VW||s.y>620||s.life<=0){s.dead=true;continue;}
            if(s.enemy&&hitPlayer(s.x,s.y)){s.dead=true;damagePlayer(s.damage);}else if(!s.enemy&&enemy.hp>0&&enemy.hit(s.x,s.y)){s.dead=true;damageEnemy(s.damage);}
        }
        if(now>enemyNext&&enemy.hp>0&&playerHP>0){float ex=enemy.x,ey=enemy.y-78;float a=(float)Math.atan2(parts.get(0).y-70-ey,parts.get(0).x-ex);shots.add(new Shot(ex-20,ey,a,590,26,true));enemyNext=now+1700+rng.nextInt(1200);}
        Iterator<Shot> it=shots.iterator();while(it.hasNext())if(it.next().dead)it.remove();
    }

    boolean hitPlayer(float x,float y){Part f=parts.get(0);return x>f.x-125&&x<f.x+125&&y>f.y-70&&y<f.y+45 || x>parts.get(6).x-45&&x<parts.get(6).x+60&&y>parts.get(6).y-40&&y<parts.get(6).y+35;}
    void damagePlayer(int d){int absorb=Math.min(playerArmor,Math.round(d*.5f));playerArmor-=absorb;playerHP=Math.max(0,playerHP-(d-absorb));}
    void damageEnemy(int d){int absorb=Math.min(enemy.armor,Math.round(d*.45f));enemy.armor-=absorb;enemy.hp=Math.max(0,enemy.hp-(d-absorb));}

    void fire(){if(playerHP<=0||enemy.hp<=0)return;Part g=parts.get(7);float a=(float)Math.toRadians(-g.angle);float sx=g.x+120*(float)Math.cos(Math.toRadians(g.angle));float sy=g.y-120*(float)Math.sin(Math.toRadians(g.angle));shots.add(new Shot(sx,sy,a,650,32,false));}

    boolean touchBuilder(MotionEvent e,float x,float y){
        if(e.getAction()==MotionEvent.ACTION_DOWN){
            if(x>895&&y>590&&x<1080){buildMode=false;return true;}
            if(x>1085&&y>590){reset();return true;}
            // Palette creates a new part at the cursor, so the player can build larger tanks.
            if(x>895&&x<1240&&y>140&&y<575){int type=paletteType(y);if(type>=0){Part a=new Part(type,650,300);a.weld=-1;parts.add(a);drag=parts.size()-1;ddx=0;ddy=0;msg="Đang kéo "+name(type);return true;}}
            for(int i=0;i<parts.size();i++){Part a=parts.get(i);if(Math.hypot(x-a.x,y-a.y)<65){drag=i;ddx=x-a.x;ddy=y-a.y;return true;}}
        }
        if((e.getAction()==MotionEvent.ACTION_MOVE||e.getAction()==MotionEvent.ACTION_UP)&&drag>=0){Part a=parts.get(drag);a.x=x-ddx;a.y=y-ddy;if(e.getAction()==MotionEvent.ACTION_UP){tryWeld(drag);drag=-1;}invalidate();return true;}
        return true;
    }
    int paletteType(float y){if(y<195)return Part.FRAME;if(y<255)return Part.MOTOR;if(y<315)return Part.ARMOR;if(y<375)return Part.BIG;if(y<435)return Part.SMALL;if(y<495)return Part.HINGE;if(y<575)return Part.GUN;return -1;}
    String name(int t){switch(t){case Part.FRAME:return "KHUNG";case Part.MOTOR:return "MOTOR";case Part.ARMOR:return "GIÁP";case Part.BIG:return "BÁNH TO";case Part.SMALL:return "BÁNH NHỎ";case Part.HINGE:return "BẢN LỀ";default:return "SÚNG";}}

    void tryWeld(int i){if(i==0)return;Part a=parts.get(i);float best=999;int parent=-1;PointF snap=null;
        for(int j=0;j<parts.size();j++)if(j!=i){for(PointF A:connectors(a))for(PointF B:connectors(parts.get(j))){float d=(float)Math.hypot(A.x-B.x,A.y-B.y);if(d<best){best=d;parent=j;snap=B;}}}
        if(best<48){PointF A=connectors(a).get(0);a.x+=snap.x-A.x;a.y+=snap.y-A.y;a.weld=parent;if(a.type==Part.GUN&&parts.get(parent).type==Part.HINGE)a.angle=hingeAngle;msg="ĐÃ HÀN: "+name(a.type)+" vào "+name(parts.get(parent).type);}
        else msg="Chưa khớp mấu nối";
    }

    boolean touchBattle(MotionEvent e,float x,float y){int a=e.getAction();if(a==MotionEvent.ACTION_DOWN||a==MotionEvent.ACTION_MOVE){left=x<120&&y>570;right=x>=120&&x<225&&y>570;up=x>=225&&x<325&&y>570;down=x>=325&&x<425&&y>570;if(a==MotionEvent.ACTION_DOWN&&x>1075&&y>570)fire();if(a==MotionEvent.ACTION_DOWN&&x>870&&x<1070&&y>570){buildMode=true;return true;}return true;}left=right=up=down=false;return true;}

    @Override public boolean onTouchEvent(MotionEvent e){float x=(e.getX()-ox)/scale,y=(e.getY()-oy)/scale;return buildMode?touchBuilder(e,x,y):touchBattle(e,x,y);}

    void txt(Canvas c,String s,float x,float y,float size,boolean bold){p.setStyle(Paint.Style.FILL);p.setColor(Color.WHITE);p.setTextSize(size);p.setTypeface(Typeface.create(Typeface.DEFAULT,bold?Typeface.BOLD:Typeface.NORMAL));c.drawText(s,x,y,p);}
    void button(Canvas c,float l,float t,float r,float b,String s,int color){p.setStyle(Paint.Style.FILL);p.setColor(color);c.drawRoundRect(l,t,r,b,18,18,p);p.setColor(Color.WHITE);p.setTextSize(22);p.setTypeface(Typeface.DEFAULT_BOLD);float w=p.measureText(s);c.drawText(s,(l+r-w)/2,t+(b-t)/2+8,p);}

    static class Part{
        static final int FRAME=0,MOTOR=1,ARMOR=2,BIG=3,SMALL=4,HINGE=5,GUN=6;
        int type,weld=-1;float x,y,angle;
        Part(int t,float x,float y){type=t;this.x=x;this.y=y;}
    }

    static class Shot{
        float x,y,vx,vy,life=5;int damage;boolean enemy,dead;
        Shot(float x,float y,float a,float speed,int damage,boolean enemy){this.x=x;this.y=y;this.vx=(float)Math.cos(a)*speed;this.vy=(float)Math.sin(a)*speed;this.damage=damage;this.enemy=enemy;}
        void draw(Canvas c){if(dead)return;Paint q=new Paint(Paint.ANTI_ALIAS_FLAG);q.setColor(Color.argb(130,255,180,50));c.drawCircle(x,y,16,q);q.setColor(Color.rgb(250,235,100));c.drawCircle(x,y,9,q);}
    }

    static class Tank{
        float x,y;boolean enemy;int hp=300,armor=130;
        Tank(float x,float y,boolean e){this.x=x;this.y=y;enemy=e;}
        boolean hit(float px,float py){return px>x-120&&px<x+120&&py>y-65&&py<y+40||px>x-50&&px<x+65&&py>y-115&&py<y-50;}
        void draw(Canvas c){Paint q=new Paint(Paint.ANTI_ALIAS_FLAG);q.setColor(Color.argb(60,0,0,0));c.drawOval(x-145,y+25,x+145,y+65,q);q.setColor(Color.rgb(55,55,58));c.drawRoundRect(x-145,y-20,x+145,y+55,28,28,q);q.setColor(Color.rgb(145,145,145));for(int i=0;i<7;i++)c.drawCircle(x-105+i*35,y+20,13,q);q.setColor(Color.rgb(125,75,60));Path h=new Path();h.moveTo(x-120,y-55);h.lineTo(x+100,y-55);h.lineTo(x+125,y+5);h.lineTo(x-105,y+5);h.close();c.drawPath(h,q);q.setColor(Color.rgb(95,60,55));c.drawRoundRect(x-50,y-108,x+60,y-50,22,22,q);q.setColor(Color.rgb(45,45,45));q.setStrokeWidth(17);q.setStrokeCap(Paint.Cap.ROUND);c.drawLine(x+12,y-78,x+145,y-78,q);q.setColor(Color.rgb(190,190,175));c.drawCircle(x+112,y-15,22,q);q.setColor(Color.argb(160,0,0,0));c.drawRect(x-120,y-150,x+120,y-136,q);q.setColor(Color.rgb(70,220,85));c.drawRect(x-120,y-150,x-120+240*Math.max(0,hp)/300f,y-136,q);}
    }
}
