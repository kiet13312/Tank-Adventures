package com.tankadventures;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.*;
import android.view.MotionEvent;
import android.view.View;
import java.util.*;

public class TankGameView extends View {
    static final int VW=1280,VH=720;
    final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    final ArrayList<Part> parts=new ArrayList<>();
    final ArrayList<Shot> shots=new ArrayList<>();
    final SharedPreferences prefs;
    Enemy enemy;
    float scale,ox,oy,gunAngle=18,worldX=420,cameraX;
    boolean buildMode=true,left,right,up,down,levelWon,levelLost;
    int drag=-1,level=1,coins=0,playerHP=430,playerArmor=170;
    float[] relX,relY;
    long last,enemyNext,enemyThink;
    String msg="Kéo bộ phận vào mấu nối để HÀN";

    public TankGameView(Context c){
        super(c);setLayerType(View.LAYER_TYPE_SOFTWARE,null);
        prefs=c.getSharedPreferences("tank_adventures",Context.MODE_PRIVATE);
        coins=prefs.getInt("coins",0);
        level=Math.max(1,Math.min(15,prefs.getInt("level",1)));
        reset();last=System.currentTimeMillis();
    }
    void reset(){
        parts.clear();
        add(Part.FRAME,260,455,-1);add(Part.MOTOR,260,390,0);add(Part.ARMOR,260,425,0);
        add(Part.BIG,170,515,0);add(Part.BIG,275,515,0);add(Part.SMALL,225,530,0);
        add(Part.HINGE,325,370,0);add(Part.GUN,380,370,6);
        parts.get(7).angle=18;gunAngle=18;makeRelative();worldX=420;
        playerHP=430;playerArmor=170;levelWon=false;levelLost=false;shots.clear();
        enemy=makeEnemy(level);enemyNext=System.currentTimeMillis()+1200;enemyThink=System.currentTimeMillis()+250;
        msg="Kéo bộ phận vào mấu nối để HÀN";
    }
    void add(int type,float x,float y,int parent){Part a=new Part(type,x,y);a.weld=parent;parts.add(a);}
    void makeRelative(){
        if(parts.isEmpty())return;Part r=parts.get(0);relX=new float[parts.size()];relY=new float[parts.size()];
        for(int i=0;i<parts.size();i++){relX[i]=parts.get(i).x-r.x;relY[i]=parts.get(i).y-r.y;}
    }
    int count(int type){int n=0;for(Part a:parts)if(a.type==type&&a.weld>=0)n++;return n;}
    Part first(int type){for(Part a:parts)if(a.type==type&&a.weld>=0)return a;return null;}
    Enemy makeEnemy(int lv){
        Enemy e=new Enemy();e.x=2250+lv*180;e.hp=250+lv*48;e.maxHp=e.hp;e.armor=100+lv*20;e.damage=22+lv*2;e.fireMs=Math.max(850,1900-lv*55);e.speed=15+lv*1.6f;e.boss=lv%5==0;
        if(e.boss){e.hp+=220;e.maxHp=e.hp;e.armor+=90;e.damage+=8;e.fireMs=Math.max(650,e.fireMs-180);e.speed+=5;}
        e.name=e.boss?"BOSS":"XE ĐỊCH "+lv;return e;
    }
    float terrainY(float x){float phase=level*.63f;return 470+55*(float)Math.sin(x/260f+phase)+32*(float)Math.sin(x/105f+level)+18*(float)Math.sin(x/53f+2);}
    float terrainSlope(float x){return (terrainY(x+2)-terrainY(x-2))/4f;}
    int levelLength(){return 4500+level*260;}

    @Override protected void onDraw(Canvas c){
        scale=Math.min(getWidth()/1280f,getHeight()/720f);ox=(getWidth()-VW*scale)/2f;oy=(getHeight()-VH*scale)/2f;c.save();c.translate(ox,oy);c.scale(scale,scale);
        long now=System.currentTimeMillis();float dt=Math.min(.033f,Math.max(0,(now-last)/1000f));last=now;
        if(buildMode)drawBuilder(c);else{if(!levelWon&&!levelLost)updateBattle(dt,now);drawBattle(c);}c.restore();postInvalidateDelayed(16);
    }
    void bg(Canvas c){
        p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(67,151,196));c.drawRect(0,0,VW,VH,p);p.setColor(Color.argb(55,255,255,255));
        c.drawCircle(170,100,35,p);c.drawCircle(215,88,48,p);c.drawCircle(263,105,32,p);c.drawCircle(850,125,34,p);c.drawCircle(895,115,52,p);c.drawCircle(952,130,30,p);
    }
    void drawBuilder(Canvas c){
        bg(c);txt(c,"TANK ADVENTURES — LẮP RÁP",30,42,34,true);txt(c,"MÀN "+level+" / 15     XU: "+coins,30,75,21,false);
        p.setColor(Color.argb(95,20,30,35));c.drawRoundRect(35,105,850,575,24,24,p);txt(c,"KHU VỰC LẮP RÁP",70,145,22,true);
        for(int i=0;i<parts.size();i++)drawPart(c,parts.get(i),i);
        p.setColor(Color.argb(220,27,32,38));c.drawRoundRect(880,95,1250,575,24,24,p);txt(c,"BỘ PHẬN",915,135,25,true);
        palette(c,"KHUNG",0,165);palette(c,"MOTOR",1,225);palette(c,"GIÁP",2,285);palette(c,"BÁNH TO",3,345);palette(c,"BÁNH NHỎ",4,405);palette(c,"BẢN LỀ",5,465);palette(c,"SÚNG",6,525);
        p.setColor(Color.argb(210,25,25,25));c.drawRoundRect(35,600,850,695,18,18,p);txt(c,msg,60,632,21,false);txt(c,"Mấu xanh = điểm nối • Bộ phận HÀN sẽ được giữ nguyên khi vào trận.",60,665,18,false);
        button(c,900,605,1075,685,"CHIẾN ĐẤU",Color.rgb(55,145,70));button(c,1095,605,1245,685,"RESET",Color.rgb(175,75,55));
    }
    void palette(Canvas c,String s,int type,float y){p.setColor(Color.rgb(58,68,76));c.drawRoundRect(905,y-25,1225,y+28,14,14,p);txt(c,s,930,y+7,20,true);p.setColor(Color.rgb(90,235,215));c.drawCircle(1190,y,13,p);}
    int partColor(int t){switch(t){case Part.FRAME:return Color.rgb(48,60,68);case Part.MOTOR:return Color.rgb(160,105,52);case Part.ARMOR:return Color.rgb(125,136,140);case Part.BIG:return Color.rgb(45,47,50);case Part.SMALL:return Color.rgb(63,65,69);case Part.HINGE:return Color.rgb(185,145,52);default:return Color.rgb(65,87,105);}}
    ArrayList<PointF> connectors(Part a){
        ArrayList<PointF> r=new ArrayList<>();if(a.type==Part.FRAME){r.add(new PointF(a.x-90,a.y));r.add(new PointF(a.x,a.y-35));r.add(new PointF(a.x+90,a.y));r.add(new PointF(a.x,a.y+35));}else if(a.type==Part.HINGE){r.add(new PointF(a.x,a.y));r.add(new PointF(a.x+28,a.y));}else r.add(new PointF(a.x,a.y));return r;
    }
    void drawPart(Canvas c,Part a,int i){
        p.setStyle(Paint.Style.FILL);p.setColor(partColor(a.type));
        if(a.type==Part.FRAME)c.drawRoundRect(a.x-130,a.y-35,a.x+130,a.y+35,18,18,p);else if(a.type==Part.MOTOR)c.drawRoundRect(a.x-55,a.y-30,a.x+55,a.y+30,14,14,p);
        else if(a.type==Part.ARMOR){Path q=new Path();q.moveTo(a.x-90,a.y+28);q.lineTo(a.x-60,a.y-30);q.lineTo(a.x+75,a.y-30);q.lineTo(a.x+95,a.y+28);q.close();c.drawPath(q,p);}
        else if(a.type==Part.BIG||a.type==Part.SMALL){float r=a.type==Part.BIG?38:25;c.drawCircle(a.x,a.y,r,p);p.setColor(Color.rgb(155,155,155));c.drawCircle(a.x,a.y,r*.48f,p);}
        else if(a.type==Part.HINGE){c.drawCircle(a.x,a.y,23,p);p.setColor(Color.rgb(40,40,40));c.drawCircle(a.x,a.y,8,p);}
        else{p.setStrokeWidth(16);p.setStrokeCap(Paint.Cap.ROUND);float r=(float)Math.toRadians(a.angle);c.drawLine(a.x,a.y,a.x+150*(float)Math.cos(r),a.y-150*(float)Math.sin(r),p);}
        p.setColor(Color.rgb(90,235,215));for(PointF q:connectors(a))c.drawCircle(q.x,q.y,7,p);if(i==drag){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);p.setColor(Color.YELLOW);c.drawCircle(a.x,a.y,48,p);p.setStyle(Paint.Style.FILL);}if(a.weld>=0)txt(c,"HÀN",a.x-17,a.y+55,14,false);
    }

    void drawBattle(Canvas c){
        drawWorld(c);txt(c,"MÀN "+level+"/15",25,34,24,true);txt(c,"HP "+playerHP+"   GIÁP "+playerArmor,25,64,22,true);txt(c,"XU "+coins,25,92,20,false);txt(c,"ENEMY "+Math.max(0,enemy.hp),1020,34,24,true);txt(c,enemy.name,1020,62,18,false);txt(c,"AI: "+enemy.aiLabel(),1020,88,16,false);txt(c,"NÒNG "+(int)gunAngle+"°",25,120,18,false);
        button(c,20,585,115,685,"◀",Color.rgb(55,80,100));button(c,125,585,220,685,"▶",Color.rgb(55,80,100));button(c,230,585,325,685,"▲",Color.rgb(125,95,55));button(c,330,585,425,685,"▼",Color.rgb(125,95,55));button(c,875,585,1060,685,"LẮP RÁP",Color.rgb(60,130,75));button(c,1080,585,1250,685,"BẮN",Color.rgb(185,55,45));
        if(levelWon){p.setColor(Color.argb(225,20,80,30));c.drawRoundRect(330,220,950,500,28,28,p);txt(c,"VICTORY!",500,300,58,true);txt(c,"+100 XU",545,350,30,true);if(level<15)button(c,490,390,790,465,"MÀN TIẾP",Color.rgb(55,145,70));else txt(c,"HOÀN THÀNH 15 MÀN!",440,410,27,true);}
        if(levelLost){p.setColor(Color.argb(225,90,25,25));c.drawRoundRect(330,220,950,500,28,28,p);txt(c,"DEFEAT",530,300,58,true);button(c,490,390,790,465,"CHƠI LẠI",Color.rgb(175,75,55));}
    }
    void drawWorld(Canvas c){bg(c);cameraX=Math.max(0,Math.min(Math.max(0,levelLength()-VW),worldX-500));c.save();c.translate(-cameraX,0);drawTerrain(c);drawFinish(c);drawPlayer(c);if(enemy.hp>0)drawEnemy(c);for(Shot s:shots)s.draw(c);c.restore();}
    void drawTerrain(Canvas c){
        float l=cameraX-100,r=cameraX+1380;p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(103,179,83));Path q=new Path();q.moveTo(l,terrainY(l));for(float x=l;x<=r;x+=20)q.lineTo(x,terrainY(x));q.lineTo(r,VH);q.lineTo(l,VH);q.close();c.drawPath(q,p);p.setColor(Color.rgb(174,220,111));Path t=new Path();t.moveTo(l,terrainY(l)-20);for(float x=l;x<=r;x+=20)t.lineTo(x,terrainY(x)-20);t.lineTo(r,terrainY(r)-20);t.lineTo(l,terrainY(l)-20);t.close();c.drawPath(t,p);
    }
    void drawFinish(Canvas c){float x=levelLength()-180,y=terrainY(x);p.setColor(Color.DKGRAY);c.drawRect(x,y-170,x+10,y,p);p.setColor(Color.RED);Path f=new Path();f.moveTo(x+10,y-170);f.lineTo(x+120,y-145);f.lineTo(x+10,y-120);f.close();c.drawPath(f,p);txt(c,"FINISH",x-20,y-190,20,true);}
    void drawPlayer(Canvas c){
        if(parts.isEmpty())return;float baseY=terrainY(worldX)-98;float slope=(float)Math.toDegrees(Math.atan(terrainSlope(worldX)));c.save();c.translate(worldX,baseY);c.rotate(slope);
        for(int i=0;i<parts.size();i++){Part a=parts.get(i);if(i==0||a.weld>=0)drawLocal(c,a,relX[i],relY[i]);}c.restore();
    }
    void drawLocal(Canvas c,Part a,float x,float y){
        p.setStyle(Paint.Style.FILL);p.setColor(partColor(a.type));if(a.type==Part.FRAME)c.drawRoundRect(x-130,y-35,x+130,y+35,18,18,p);else if(a.type==Part.MOTOR)c.drawRoundRect(x-55,y-30,x+55,y+30,14,14,p);
        else if(a.type==Part.ARMOR){Path q=new Path();q.moveTo(x-90,y+28);q.lineTo(x-60,y-30);q.lineTo(x+75,y-30);q.lineTo(x+95,y+28);q.close();c.drawPath(q,p);}else if(a.type==Part.BIG||a.type==Part.SMALL){float r=a.type==Part.BIG?40:27;c.drawCircle(x,y,r,p);p.setColor(Color.rgb(145,145,145));c.drawCircle(x,y,r*.48f,p);}else if(a.type==Part.HINGE){c.drawCircle(x,y,20,p);p.setColor(Color.rgb(40,40,40));c.drawCircle(x,y,8,p);}else{p.setColor(Color.rgb(45,45,45));p.setStrokeWidth(17);p.setStrokeCap(Paint.Cap.ROUND);float r=(float)Math.toRadians(gunAngle);c.drawLine(x,y,x+155*(float)Math.cos(r),y-155*(float)Math.sin(r),p);}
    }
    void drawEnemy(Canvas c){
        float x=enemy.x,y=terrainY(x)-35;p.setStyle(Paint.Style.FILL);p.setColor(Color.argb(60,0,0,0));c.drawOval(x-145,y+25,x+145,y+65,p);p.setColor(Color.rgb(55,55,58));c.drawRoundRect(x-145,y-20,x+145,y+55,28,28,p);p.setColor(Color.rgb(145,145,145));for(int i=0;i<7;i++)c.drawCircle(x-105+i*35,y+20,13,p);
        p.setColor(enemy.boss?Color.rgb(115,55,55):Color.rgb(125,75,60));Path h=new Path();h.moveTo(x-120,y-55);h.lineTo(x+100,y-55);h.lineTo(x+125,y+5);h.lineTo(x-105,y+5);h.close();c.drawPath(h,p);p.setColor(enemy.boss?Color.rgb(75,45,45):Color.rgb(95,60,55));c.drawRoundRect(x-50,y-108,x+60,y-50,22,22,p);p.setColor(Color.rgb(45,45,45));p.setStrokeWidth(17);p.setStrokeCap(Paint.Cap.ROUND);c.drawLine(x+12,y-78,x+145,y-78,p);p.setColor(Color.rgb(190,190,175));c.drawCircle(x+112,y-15,22,p);p.setColor(Color.argb(160,0,0,0));c.drawRect(x-120,y-150,x+120,y-136,p);p.setColor(Color.rgb(70,220,85));c.drawRect(x-120,y-150,x-120+240*Math.max(0,enemy.hp)/(float)enemy.maxHp,y-136,p);
    }
    void updateBattle(float dt,long now){
        float speed=135+count(Part.MOTOR)*55;if(right)worldX=Math.min(levelLength()-250,worldX+speed*dt);if(left)worldX=Math.max(120,worldX-speed*dt);if(up)gunAngle=Math.min(78,gunAngle+55*dt);if(down)gunAngle=Math.max(5,gunAngle-55*dt);for(Part a:parts)if(a.type==Part.GUN)a.angle=gunAngle;
        if(enemy.hp>0)updateEnemyAI(dt,now);
        for(Shot s:shots){if(s.dead)continue;s.vy+=760*dt;s.x+=s.vx*dt;s.y+=s.vy*dt;s.life-=dt;if(s.y>terrainY(s.x)-5){s.y=terrainY(s.x)-5;s.dead=true;continue;}if(s.life<=0){s.dead=true;continue;}if(s.enemy&&hitPlayer(s.x,s.y)){s.dead=true;damagePlayer(s.damage);}else if(!s.enemy&&enemy.hp>0&&enemy.hit(s.x,s.y)){s.dead=true;damageEnemy(s.damage);}}
        Iterator<Shot>it=shots.iterator();while(it.hasNext())if(it.next().dead)it.remove();if(enemy.hp<=0&&!levelWon){levelWon=true;coins+=100;prefs.edit().putInt("coins",coins).apply();}if(playerHP<=0)levelLost=true;
    }
    void updateEnemyAI(float dt,long now){
        float distance=enemy.x-worldX;int desiredMode;if(distance>1050)desiredMode=0;else if(distance<520)desiredMode=2;else desiredMode=1;
        if(now>=enemyThink){enemy.aiMode=desiredMode;if(Math.abs(distance)<900&&!enemy.boss&&Math.random()<0.25)enemy.strafe=-enemy.strafe;if(enemy.boss&&Math.random()<0.30)enemy.strafe=-enemy.strafe;enemyThink=now+260;}
        float move=0;if(enemy.aiMode==0)move=-enemy.speed;else if(enemy.aiMode==2)move=enemy.speed;else move=enemy.speed*0.35f*enemy.strafe;
        for(Shot s:shots){if(!s.enemy&&!s.dead&&Math.abs(s.x-enemy.x)<260){float futureY=s.y+s.vy*0.18f;float ey=terrainY(enemy.x)-70;if(Math.abs(futureY-ey)<95){move+=(s.x<enemy.x?enemy.speed*0.9f:-enemy.speed*0.9f);break;}}}
        enemy.x+=move*dt;enemy.x=Math.max(worldX+430,Math.min(levelLength()-260,enemy.x));
        float ex=enemy.x-125,ey=terrainY(enemy.x)-110;float playerVelocity=(right?135+count(Part.MOTOR)*55:0)-(left?135+count(Part.MOTOR)*55:0);float dx=worldX-ex;float dy=(terrainY(worldX)-90)-ey;float flight=Math.max(0.35f,Math.abs(dx)/590f);float predictedX=worldX+playerVelocity*flight;predictedX=Math.max(120,Math.min(levelLength()-250,predictedX));dx=predictedX-ex;dy=(terrainY(predictedX)-90)-ey;float vx=590*Math.signum(dx);if(Math.abs(dx)<1)vx=590;float vy=(dy-0.5f*760*flight*flight)/flight;enemy.aiAim=(float)Math.atan2(vy,vx);
        if(now>=enemyNext&&playerHP>0&&enemy.hp>0){float range=Math.abs(worldX-enemy.x);if(range<1750){shots.add(new Shot(ex,ey,enemy.aiAim,590,enemy.damage,true));if(enemy.boss&&enemy.burst<2){enemy.burst++;enemyNext=now+260;}else{enemy.burst=0;enemyNext=now+enemy.fireMs;}}else enemyNext=now+250;}
    }
    boolean hitPlayer(float x,float y){float py=terrainY(worldX)-98;return Math.abs(x-worldX)<155&&y>py-130&&y<py+55;}
    void damagePlayer(int d){int a=Math.min(playerArmor,Math.round(d*.5f));playerArmor-=a;playerHP=Math.max(0,playerHP-(d-a));}
    void damageEnemy(int d){int a=Math.min(enemy.armor,Math.round(d*.45f));enemy.armor-=a;enemy.hp=Math.max(0,enemy.hp-(d-a));}
    void fire(){
        if(playerHP<=0||enemy.hp<=0||levelWon||levelLost)return;Part g=first(Part.GUN);if(g==null)return;int gi=parts.indexOf(g);float gx=relX[gi],gy=relY[gi];float slope=(float)Math.toDegrees(Math.atan(terrainSlope(worldX)));float worldAngle=slope-gunAngle;float rad=(float)Math.toRadians(worldAngle);float sx=worldX+gx+(float)Math.cos(rad)*155;float sy=terrainY(worldX)-98+gy-(float)Math.sin(Math.toRadians(gunAngle))*155;shots.add(new Shot(sx,sy,rad,650+count(Part.MOTOR)*30,32,false));
    }
    boolean touchBuilder(MotionEvent e,float x,float y){
        int a=e.getActionMasked();if(a==MotionEvent.ACTION_DOWN){if(x>895&&y>590&&x<1080){makeRelative();buildMode=false;return true;}if(x>1085&&y>590){reset();return true;}if(x>895&&x<1240&&y>140&&y<575){int t=paletteType(y);if(t>=0){Part z=new Part(t,650,300);z.weld=-1;parts.add(z);drag=parts.size()-1;msg="Đang kéo "+name(t);return true;}}for(int i=parts.size()-1;i>=0;i--){Part z=parts.get(i);if(Math.hypot(x-z.x,y-z.y)<72){drag=i;return true;}}}if((a==MotionEvent.ACTION_MOVE||a==MotionEvent.ACTION_UP)&&drag>=0){Part z=parts.get(drag);z.x=x;z.y=y;if(a==MotionEvent.ACTION_UP){tryWeld(drag);drag=-1;}invalidate();return true;}return true;
    }
    int paletteType(float y){if(y<195)return 0;if(y<255)return 1;if(y<315)return 2;if(y<375)return 3;if(y<435)return 4;if(y<495)return 5;if(y<575)return 6;return -1;}
    String name(int t){switch(t){case 0:return "KHUNG";case 1:return "MOTOR";case 2:return "GIÁP";case 3:return "BÁNH TO";case 4:return "BÁNH NHỎ";case 5:return "BẢN LỀ";default:return "SÚNG";}}
    void tryWeld(int i){
        if(i==0)return;Part a=parts.get(i);float best=999;int parent=-1;PointF aa=null,bb=null;for(int j=0;j<parts.size();j++)if(j!=i)for(PointF A:connectors(a))for(PointF B:connectors(parts.get(j))){float d=(float)Math.hypot(A.x-B.x,A.y-B.y);if(d<best){best=d;parent=j;aa=A;bb=B;}}if(best<55){a.x+=bb.x-aa.x;a.y+=bb.y-aa.y;a.weld=parent;if(a.type==Part.GUN&&parts.get(parent).type==Part.HINGE)a.angle=gunAngle;msg="ĐÃ HÀN: "+name(a.type)+" vào "+name(parts.get(parent).type);}else msg="Chưa khớp mấu nối";
    }
    boolean touchBattle(MotionEvent e,float x,float y){
        int a=e.getActionMasked();if(a==MotionEvent.ACTION_DOWN||a==MotionEvent.ACTION_MOVE){left=x<120&&y>570;right=x>=120&&x<225&&y>570;up=x>=225&&x<325&&y>570;down=x>=325&&x<425&&y>570;if(a==MotionEvent.ACTION_DOWN&&x>1075&&y>570){fire();return true;}if(a==MotionEvent.ACTION_DOWN&&x>870&&x<1070&&y>570){makeRelative();buildMode=true;return true;}if(a==MotionEvent.ACTION_DOWN&&levelWon&&level<15&&x>480&&x<800&&y>380&&y<480){nextLevel();return true;}if(a==MotionEvent.ACTION_DOWN&&levelLost&&x>480&&x<800&&y>380&&y<480){retryLevel();return true;}return true;}left=right=up=down=false;return true;
    }
    void nextLevel(){level=Math.min(15,level+1);prefs.edit().putInt("level",level).apply();reset();buildMode=false;}
    void retryLevel(){reset();buildMode=false;}
    @Override public boolean onTouchEvent(MotionEvent e){float x=(e.getX()-ox)/scale,y=(e.getY()-oy)/scale;return buildMode?touchBuilder(e,x,y):touchBattle(e,x,y);}
    void txt(Canvas c,String s,float x,float y,float size,boolean bold){p.setStyle(Paint.Style.FILL);p.setColor(Color.WHITE);p.setTextSize(size);p.setTypeface(Typeface.create(Typeface.DEFAULT,bold?Typeface.BOLD:Typeface.NORMAL));c.drawText(s,x,y,p);}
    void button(Canvas c,float l,float t,float r,float b,String s,int color){p.setStyle(Paint.Style.FILL);p.setColor(color);c.drawRoundRect(l,t,r,b,18,18,p);p.setColor(Color.WHITE);p.setTextSize(22);p.setTypeface(Typeface.DEFAULT_BOLD);float w=p.measureText(s);c.drawText(s,(l+r-w)/2,t+(b-t)/2+8,p);}
    static class Part{static final int FRAME=0,MOTOR=1,ARMOR=2,BIG=3,SMALL=4,HINGE=5,GUN=6;int type,weld=-1;float x,y,angle;Part(int t,float x,float y){type=t;this.x=x;this.y=y;}}
    static class Shot{float x,y,vx,vy,life=5;int damage;boolean enemy,dead;Shot(float x,float y,float a,float speed,int d,boolean e){this.x=x;this.y=y;vx=(float)Math.cos(a)*speed;vy=(float)Math.sin(a)*speed;damage=d;enemy=e;}void draw(Canvas c){if(dead)return;Paint q=new Paint(Paint.ANTI_ALIAS_FLAG);q.setColor(Color.argb(130,255,180,50));c.drawCircle(x,y,16,q);q.setColor(Color.rgb(250,235,100));c.drawCircle(x,y,9,q);}}
    class Enemy{float x,speed,aiAim;int hp,maxHp,armor,damage,fireMs;boolean boss;String name="";int aiMode=1,strafe=1,burst=0;boolean hit(float px,float py){float gy=terrainY(x)-35;return px>x-130&&px<x+130&&py>gy-115&&py<gy+60;}String aiLabel(){if(aiMode==0)return "TIẾN CÔNG";if(aiMode==2)return "LÙI / NÉ";return "GIỮ KHOẢNG CÁCH";}}
}
