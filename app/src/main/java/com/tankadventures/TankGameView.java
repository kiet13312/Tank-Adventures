package com.tankadventures;

import android.content.Context;
import android.graphics.*;
import android.view.MotionEvent;
import android.view.View;
import java.util.*;

/**
 * Tank Adventures prototype.
 * Original implementation of a modular tank builder + side-view artillery battle.
 * It intentionally uses original simple shapes rather than copying another game's assets.
 */
public class TankGameView extends View {
    static final int VW=1280, VH=720;
    final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    final Random random=new Random();
    final ArrayList<Projectile> shots=new ArrayList<>();
    final ArrayList<Part> parts=new ArrayList<>();
    Tank enemy;
    float scale,ox,oy;
    long last,enemyNext;
    boolean assembly=true, movingLeft,movingRight,hingeUp,hingeDown;
    int selectedPart=-1;
    float dragDX,dragDY;
    float hingeAngle=18;
    String message="Kéo bộ phận vào mấu nối để hàn";

    public TankGameView(Context c){
        super(c);setLayerType(View.LAYER_TYPE_SOFTWARE,null);
        resetBuild();
        enemy=new Tank(1030,500,true);
        last=System.currentTimeMillis();enemyNext=last+1600;
    }

    void resetBuild(){
        parts.clear();
        // Main frame and the first mounted components.
        parts.add(new Part(Part.FRAME,250,475,0));
        parts.add(new Part(Part.MOTOR,250,410,0));
        parts.add(new Part(Part.ARMOR,250,440,0));
        parts.add(new Part(Part.WHEEL_BIG,170,520,0));
        parts.add(new Part(Part.WHEEL_BIG,270,520,0));
        parts.add(new Part(Part.WHEEL_SMALL,215,535,0));
        parts.add(new Part(Part.HINGE,300,385,0));
        parts.add(new Part(Part.GUN,355,385,hingeAngle));
        // connectors: index -> index; these are logical welds.
        parts.get(1).weldTo=0;
        parts.get(2).weldTo=0;
        parts.get(3).weldTo=0;
        parts.get(4).weldTo=0;
        parts.get(5).weldTo=0;
        parts.get(6).weldTo=0;
        parts.get(7).weldTo=6;
    }

    @Override protected void onDraw(Canvas out){
        scale=Math.min(getWidth()/1280f,getHeight()/720f);
        ox=(getWidth()-VW*scale)/2f;oy=(getHeight()-VH*scale)/2f;
        out.save();out.translate(ox,oy);out.scale(scale,scale);
        background(out);
        long now=System.currentTimeMillis();
        float dt=Math.min(.033f,(now-last)/1000f);last=now;
        if(!assembly) updateBattle(dt,now);
        if(assembly) drawAssembly(out); else drawBattle(out);
        out.restore();postInvalidateDelayed(16);
    }

    void background(Canvas c){
        p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(72,154,198));c.drawRect(0,0,VW,720,p);
        p.setColor(Color.rgb(103,178,85));c.drawRect(0,520,VW,720,p);
        p.setColor(Color.rgb(174,220,111));c.drawRect(0,520,VW,548,p);
        p.setColor(Color.argb(85,255,255,255));
        c.drawCircle(165,105,35,p);c.drawCircle(210,92,48,p);c.drawCircle(258,108,32,p);
        c.drawCircle(845,130,35,p);c.drawCircle(895,118,52,p);c.drawCircle(950,135,31,p);
    }

    void drawAssembly(Canvas c){
        p.setColor(Color.WHITE);p.setTextSize(34);p.setTypeface(Typeface.DEFAULT_BOLD);
        c.drawText("TANK ADVENTURES — LẮP RÁP",30,45,p);
        p.setTextSize(21);p.setTypeface(Typeface.DEFAULT);
        c.drawText("Kéo bộ phận → đưa mấu nối gần nhau → tự động HÀN",30,78,p);

        // Builder work area.
        p.setColor(Color.argb(80,20,30,35));c.drawRoundRect(40,105,850,575,25,25,p);
        p.setColor(Color.argb(70,255,255,255));c.drawRoundRect(65,130,825,550,20,20,p);
        p.setColor(Color.WHITE);p.setTextSize(22);c.drawText("KHU VỰC LẮP RÁP",90,165,p);

        for(int i=0;i<parts.size();i++) drawPart(c,parts.get(i),i);

        // Palette.
        p.setColor(Color.argb(210,30,35,40));c.drawRoundRect(880,100,1250,575,25,25,p);
        p.setColor(Color.WHITE);p.setTextSize(26);c.drawText("BỘ PHẬN",925,140,p);
        drawPalette(c,"KHUNG",0,170);drawPalette(c,"MOTOR",1,235);drawPalette(c,"GIÁP",2,300);
        drawPalette(c,"BÁNH TO",3,365);drawPalette(c,"BÁNH NHỎ",4,430);drawPalette(c,"BẢN LỀ",5,495);
        drawPalette(c,"SÚNG",6,560);

        p.setColor(Color.argb(210,25,25,25));c.drawRoundRect(40,600,825,695,20,20,p);
        p.setColor(Color.WHITE);p.setTextSize(22);c.drawText(message,65,630,p);
        c.drawText("Các mấu tròn = điểm nối / hàn",65,662,p);
        button(c,920,605,1080,685,"CHIẾN ĐẤU",Color.rgb(55,145,70));
        button(c,1090,605,1250,685,"RESET",Color.rgb(175,75,55));
    }

    void drawPalette(Canvas c,String name,int type,float y){
        p.setColor(Color.argb(230,55,65,72));c.drawRoundRect(905,y-25,1225,y+30,15,15,p);
        p.setColor(Color.WHITE);p.setTextSize(20);c.drawText(name,930,y+7,p);
        p.setColor(Color.rgb(95,170,190));c.drawCircle(1190,y,15,p);
    }

    void drawPart(Canvas c,Part a,int index){
        p.setStyle(Paint.Style.FILL);
        int base;
        switch(a.type){
            case Part.FRAME: base=Color.rgb(48,60,68);break;
            case Part.MOTOR: base=Color.rgb(155,105,55);break;
            case Part.ARMOR: base=Color.rgb(125,135,138);break;
            case Part.WHEEL_BIG: base=Color.rgb(45,48,52);break;
            case Part.WHEEL_SMALL: base=Color.rgb(62,65,70);break;
            case Part.HINGE: base=Color.rgb(180,145,55);break;
            default: base=Color.rgb(70,90,105);
        }
        p.setColor(base);
        if(a.type==Part.FRAME)c.drawRoundRect(a.x-130,a.y-35,a.x+130,a.y+35,18,18,p);
        else if(a.type==Part.MOTOR)c.drawRoundRect(a.x-55,a.y-30,a.x+55,a.y+30,15,15,p);
        else if(a.type==Part.ARMOR){Path q=new Path();q.moveTo(a.x-90,a.y+25);q.lineTo(a.x-60,a.y-30);q.lineTo(a.x+75,a.y-30);q.lineTo(a.x+95,a.y+25);q.close();c.drawPath(q,p);}
        else if(a.type==Part.WHEEL_BIG||a.type==Part.WHEEL_SMALL){float r=a.type==Part.WHEEL_BIG?38:25;c.drawCircle(a.x,a.y,r,p);p.setColor(Color.rgb(145,145,145));c.drawCircle(a.x,a.y,r*.48f,p);}
        else if(a.type==Part.HINGE){c.drawCircle(a.x,a.y,22,p);p.setColor(Color.rgb(45,45,45));c.drawCircle(a.x,a.y,8,p);}
        else { // gun
            p.setStrokeWidth(16);p.setStrokeCap(Paint.Cap.ROUND);c.drawLine(a.x,a.y,a.x+150*(float)Math.cos(Math.toRadians(a.angle)),a.y-150*(float)Math.sin(Math.toRadians(a.angle)),p);
        }
        // connection points
        p.setColor(Color.rgb(90,235,215));
        for(PointF pt:connectors(a))c.drawCircle(pt.x,pt.y,7,p);
        if(index==selectedPart){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);p.setColor(Color.YELLOW);c.drawCircle(a.x,a.y,50,p);p.setStyle(Paint.Style.FILL);}
        if(a.weldTo>=0){p.setColor(Color.rgb(90,235,215));p.setTextSize(15);c.drawText("WELDED",a.x-28,a.y+55,p);}
    }

    ArrayList<PointF> connectors(Part a){
        ArrayList<PointF> z=new ArrayList<>();
        if(a.type==Part.FRAME){z.add(new PointF(a.x-90,a.y));z.add(new PointF(a.x,a.y-35));z.add(new PointF(a.x+80,a.y));z.add(new PointF(a.x,a.y+35));}
        else if(a.type==Part.MOTOR||a.type==Part.ARMOR){z.add(new PointF(a.x,a.y+30));z.add(new PointF(a.x,a.y-30));}
        else if(a.type==Part.WHEEL_BIG||a.type==Part.WHEEL_SMALL){z.add(new PointF(a.x,a.y));}
        else if(a.type==Part.HINGE){z.add(new PointF(a.x,a.y));z.add(new PointF(a.x+30,a.y));}
        else {z.add(new PointF(a.x,a.y));}
        return z;
    }

    void drawBattle(Canvas c){
        drawBuiltTank(c);
        enemy.draw(c);
        for(Projectile s:shots)s.draw(c);
        p.setTypeface(Typeface.DEFAULT_BOLD);p.setColor(Color.WHITE);p.setTextSize(28);
        c.drawText("HP "+builtHP()+"   ARMOR "+builtArmor(),28,40,p);
        c.drawText("ENEMY "+enemy.hp,1050,40,p);
        p.setTypeface(Typeface.DEFAULT);p.setTextSize(19);c.drawText("NÒNG: "+(int)hingeAngle+"°",28,70,p);
        button(c,25,585,115,680,"◀",Color.rgb(55,80,100));
        button(c,125,585,215,680,"▶",Color.rgb(55,80,100));
        button(c,225,585,315,680,"▲",Color.rgb(120,95,55));
        button(c,325,585,415,680,"▼",Color.rgb(120,95,55));
        button(c,1080,585,1245,680,"BẮN",Color.rgb(185,55,45));
        button(c,890,585,1065,680,"LẮP RÁP",Color.rgb(60,130,75));
        if(enemy.hp<=0){p.setTextSize(60);c.drawText("VICTORY!",515,300,p);}
        if(builtHP()<=0){p.setTextSize(60);c.drawText("DEFEAT",540,300,p);}
    }

    void drawBuiltTank(Canvas c){
        float dx=0;
        for(Part a:parts){
            if(a.type==Part.WHEEL_BIG||a.type==Part.WHEEL_SMALL)continue;
        }
        // Draw only the welded combat assembly around its frame position.
        Part f=parts.get(0);float x=f.x,y=f.y;
        p.setColor(Color.argb(65,0,0,0));c.drawOval(x-155,y+35,x+155,y+70,p);
        for(Part a:parts){
            if(a.type==Part.WHEEL_BIG||a.type==Part.WHEEL_SMALL){
                float r=a.type==Part.WHEEL_BIG?40:27;
                p.setColor(Color.rgb(42,45,48));c.drawCircle(a.x,a.y,r,p);
                p.setColor(Color.rgb(150,150,150));c.drawCircle(a.x,a.y,r*.5f,p);
            }
        }
        p.setColor(Color.rgb(48,60,68));c.drawRoundRect(x-140,y-45,x+140,y+35,20,20,p);
        p.setColor(Color.rgb(125,135,138));c.drawRoundRect(x-95,y-65,x+80,y-20,15,15,p);
        Part h=parts.get(6);Part g=parts.get(7);
        p.setColor(Color.rgb(180,145,55));c.drawCircle(h.x,h.y,18,p);
        p.setColor(Color.rgb(55,55,58));p.setStrokeWidth(17);p.setStrokeCap(Paint.Cap.ROUND);
        c.drawLine(h.x,h.y,g.x+150*(float)Math.cos(Math.toRadians(g.angle)),g.y-150*(float)Math.sin(Math.toRadians(g.angle)),p);
        p.setColor(Color.rgb(170,110,55));c.drawRoundRect(x-45,y-100,x+50,y-55,15,15,p);
    }

    int builtHP(){return 300 + count(Part.ARMOR)*40 + count(Part.FRAME)*60;}
    int builtArmor(){return 80 + count(Part.ARMOR)*45;}
    int count(int type){int n=0;for(Part a:parts)if(a.type==type&&a.weldTo>=0)n++;return n;}

    void updateBattle(float dt,long now){
        Part frame=parts.get(0);
        float speed=190;
        if(movingLeft)frame.x=Math.max(160,frame.x-speed*dt);
        if(movingRight)frame.x=Math.min(700,frame.x+speed*dt);
        // Welded components follow the frame.
        float oldX=frame.x;
        for(Part a:parts){
            if(a==frame)continue;
            if(a.weldTo==0){/* positions are kept relative by moveAssembly below */}
        }
        syncWelds();
        if(hingeUp)hingeAngle=Math.min(75,hingeAngle+50*dt);
        if(hingeDown)hingeAngle=Math.max(5,hingeAngle-50*dt);
        parts.get(7).angle=hingeAngle;
        for(Projectile s:shots){
            if(s.dead)continue;s.vy+=760*dt;s.x+=s.vx*dt;s.y+=s.vy*dt;s.life-=dt;
            if(s.x<0||s.x>VW||s.y>620||s.life<=0){s.dead=true;continue;}
            Tank target=s.owner==null?enemy:playerTankProxy();
            if(target!=null&&target.hp>0&&target.hit(s.x,s.y)){s.dead=true;applyDamage(target,s.damage);}
        }
        if(now>enemyNext&&enemy.hp>0&&builtHP()>0){
            float dx=parts.get(0).x-enemy.x,dy=(parts.get(0).y-65)-(enemy.y-75);
            float a=(float)Math.atan2(dy,dx);
            shots.add(Projectile.fire(enemy.x-20,enemy.y-78,a,590,24,enemy));
            enemyNext=now+1800+random.nextInt(1100);
        }
        Iterator<Projectile> it=shots.iterator();while(it.hasNext())if(it.next().dead)it.remove();
    }

    Tank playerTankProxy(){
        // The modular player has a simple combat HP proxy for projectile collision.
        return playerProxy;
    }
    final Tank playerProxy=new Tank(250,475,false);
    void applyDamage(Tank t,int attack){
        if(t==playerProxy)return;
        int absorbed=Math.min(t.armor,Math.round(attack*.45f));t.armor-=absorbed;t.hp=Math.max(0,t.hp-(attack-absorbed));
    }

    void fire(){
        if(builtHP()<=0)return;
        Part f=parts.get(0),g=parts.get(7);
        float sx=g.x+120*(float)Math.cos(Math.toRadians(g.angle));
        float sy=g.y-120*(float)Math.sin(Math.toRadians(g.angle));
        float a=(float)Math.toRadians(-g.angle);
        shots.add(Projectile.fire(sx,sy,a,650,30,null));
    }

    void syncWelds(){
        Part f=parts.get(0);float fx=f.x,fy=f.y;
        float[][] rel={{0,0},{0,-65},{0,-35},{-80,45},{20,45},{-25,60},{75,-110},{130,-110}};
        for(int i=1;i<parts.size();i++)if(parts.get(i).weldTo==0){parts.get(i).x=fx+rel[i][0];parts.get(i).y=fy+rel[i][1];}
        parts.get(7).x=parts.get(6).x+55;parts.get(7).y=parts.get(6).y;
    }

    void button(Canvas c,float l,float t,float r,float b,String text,int color){
        p.setColor(color);c.drawRoundRect(l,t,r,b,18,18,p);p.setColor(Color.WHITE);p.setTextSize(22);p.setTypeface(Typeface.DEFAULT_BOLD);
        float tw=p.measureText(text);c.drawText(text,(l+r-tw)/2,t+(b-t)/2+8,p);
    }

    @Override public boolean onTouchEvent(MotionEvent e){
        float x=(e.getX()-ox)/scale,y=(e.getY()-oy)/scale;
        if(assembly)return touchAssembly(e,x,y);
        return touchBattle(e,x,y);
    }

    boolean touchAssembly(MotionEvent e,float x,float y){
        if(e.getAction()==MotionEvent.ACTION_DOWN){
            if(x>=920&&y>=600){assembly=false;syncWelds();return true;}
            if(x>=1090&&y>=600){resetBuild();message="Đã reset tank";return true;}
            for(int i=0;i<parts.size();i++){
                Part a=parts.get(i);
                if(Math.hypot(x-a.x,y-a.y)<65){selectedPart=i;dragDX=x-a.x;dragDY=y-a.y;return true;}
            }
        }
        if((e.getAction()==MotionEvent.ACTION_MOVE||e.getAction()==MotionEvent.ACTION_UP)&&selectedPart>=0){
            Part a=parts.get(selectedPart);a.x=x-dragDX;a.y=y-dragDY;
            if(e.getAction()==MotionEvent.ACTION_UP){tryWeld(selectedPart);selectedPart=-1;}
            invalidate();return true;
        }
        return true;
    }

    void tryWeld(int idx){
        Part a=parts.get(idx);if(idx==0)return;
        float best=999;int bestOwner=-1;
        for(int j=0;j<parts.size();j++)if(j!=idx){
            for(PointF A:connectors(a))for(PointF B:connectors(parts.get(j))){float d=(float)Math.hypot(A.x-B.x,A.y-B.y);if(d<best){best=d;bestOwner=j;}}
        }
        if(best<45){a.weldTo=bestOwner;message="ĐÃ HÀN: "+name(a.type)+" ↔ "+name(parts.get(bestOwner).type);if(bestOwner==0)syncWelds();}
        else message="Chưa vào đúng mấu nối";
    }

    boolean touchBattle(MotionEvent e,float x,float y){
        int act=e.getAction();
        if(act==MotionEvent.ACTION_DOWN||act==MotionEvent.ACTION_MOVE){
            movingLeft=x<115&&y>570;movingRight=x>=115&&x<220&&y>570;
            hingeUp=x>=215&&x<320&&y>570;hingeDown=x>=320&&x<425&&y>570;
            if(act==MotionEvent.ACTION_DOWN&&x>1060&&y>570)fire();
            if(act==MotionEvent.ACTION_DOWN&&x>875&&x<1075&&y>570){assembly=true;return true;}
            return true;
        }
        movingLeft=movingRight=hingeUp=hingeDown=false;return true;
    }

    String name(int t){switch(t){case Part.FRAME:return "KHUNG";case Part.MOTOR:return "MOTOR";case Part.ARMOR:return "GIÁP";case Part.WHEEL_BIG:return "BÁNH TO";case Part.WHEEL_SMALL:return "BÁNH NHỎ";case Part.HINGE:return "BẢN LỀ";default:return "SÚNG";}}

    static class Part{
        static final int FRAME=0,MOTOR=1,ARMOR=2,WHEEL_BIG=3,WHEEL_SMALL=4,HINGE=5,GUN=6;
        int type,weldTo=-1;float x,y,angle;
        Part(int type,float x,float y,float angle){this.type=type;this.x=x;this.y=y;this.angle=angle;}
    }

    static class Projectile{
        float x,y,vx,vy,life=5;int damage;boolean dead;Tank owner;
        static Projectile fire(float x,float y,float a,float speed,int damage,Tank owner){Projectile z=new Projectile();z.x=x;z.y=y;z.vx=(float)Math.cos(a)*speed;z.vy=(float)Math.sin(a)*speed;z.damage=damage;z.owner=owner;return z;}
        void draw(Canvas c){if(dead)return;Paint q=new Paint(Paint.ANTI_ALIAS_FLAG);q.setColor(Color.argb(130,255,180,50));c.drawCircle(x,y,16,q);q.setColor(Color.rgb(250,235,100));c.drawCircle(x,y,9,q);}
    }

    static class Tank{
        float x,y;boolean enemyTank;int hp=240,armor=120;
        Tank(float x,float y,boolean e){this.x=x;this.y=y;enemyTank=e;}
        boolean hit(float px,float py){return(px>x-120&&px<x+120&&py>y-65&&py<y+35)||(px>x-50&&px<x+65&&py>y-115&&py<y-50);}
        void draw(Canvas c){
            Paint q=new Paint(Paint.ANTI_ALIAS_FLAG);
            q.setColor(Color.argb(65,0,0,0));c.drawOval(x-145,y+22,x+145,y+65,q);
            q.setColor(Color.rgb(55,55,58));c.drawRoundRect(x-145,y-20,x+145,y+55,28,28,q);
            q.setColor(Color.rgb(145,145,145));for(int i=0;i<7;i++)c.drawCircle(x-105+i*35,y+20,13,q);
            q.setColor(Color.rgb(125,75,60));Path h=new Path();h.moveTo(x-120,y-55);h.lineTo(x+100,y-55);h.lineTo(x+125,y+5);h.lineTo(x-105,y+5);h.close();c.drawPath(h,q);
            q.setColor(Color.rgb(95,60,55));c.drawRoundRect(x-50,y-108,x+60,y-50,22,22,q);
            q.setColor(Color.rgb(45,45,45));q.setStrokeWidth(17);q.setStrokeCap(Paint.Cap.ROUND);c.drawLine(x+12,y-78,x+145,y-78,q);
            q.setColor(Color.rgb(190,190,175));c.drawCircle(x+112,y-15,22,q);
            q.setColor(Color.argb(160,0,0,0));c.drawRect(x-120,y-150,x+120,y-136,q);q.setColor(Color.rgb(70,220,85));c.drawRect(x-120,y-150,x-120+240*(Math.max(0,hp)/240f),y-136,q);
        }
    }
}
