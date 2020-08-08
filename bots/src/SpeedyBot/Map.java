package SpeedyBot;

import java.util.HashMap;
import java.util.LinkedList;

public class Map {
    private Field[][] map;
    private int rows;
    private int cols;
    private  LinkedList<Offset> viewBorder;
    private  LinkedList<Offset> view;
    private  LinkedList<Offset> attackArea;
    private  LinkedList<Offset> attackBorder;
    private  LinkedList<Offset> attackBorder2;
    private  LinkedList<Offset> visitArea;
    private  LinkedList<Offset> visitBorder;
    private  HashMap<Offset, LinkedList<Offset>> attackOffsets;

    public LinkedList<Offset> getViewBorder(){return viewBorder;}
    public LinkedList<Offset> getView(){return view;}
    public LinkedList<Offset> getAttackArea(){return attackArea;}
    public LinkedList<Offset> getAttackBorder(){return attackBorder;}
    public LinkedList<Offset> getAttackBorder2(){return attackBorder2;}
    public LinkedList<Offset> getVisitArea(){return visitArea;}
    public LinkedList<Offset> getVisitBorder(){return visitBorder;}
    public HashMap<Offset, LinkedList<Offset>> getAttackOffsets(){return attackOffsets;};

    public Map(int rows, int cols,int viewRadius2, int attackRadius2){
        map = new Field[cols][rows];
        for (int i = 0; i < cols; i++) {
            for (int j = 0; j < rows; j++) {
                map[i][j] = new Field(i, j, this);
            }
        }
        this.rows=rows;
        this.cols=cols;
        loadOffsets(viewRadius2, attackRadius2, 5);
    }

    public  Field getField(int x, int y) {
        return map[x][y];
    }

    public  void turnReset() {
        for (int i = 0; i < cols; i++) {
            for (int j = 0; j < rows; j++) {
                Field field = map[i][j];
                field.reset();
            }
        }
    }

    public  void setWater(int x, int y) {
        map[x][y].setWater();
    }

    public  int getRows() {
        return rows;
    }

    public  int getCols() {
        return cols;
    }

    public int getVisibleFields() {
        int visibleFieldCount =0;
        for (int i = 0; i < cols; i++) {
            for (int j = 0; j < rows; j++) {
                Field field = map[i][j];
                visibleFieldCount += field.isVisible() ? 1:0;
            }
        }
        return visibleFieldCount;
    }



    private  void loadOffsets(int viewRadius2, int attackRadius2, int visitRadius2) {
        view = new LinkedList<Offset>();
        viewBorder = new LinkedList<Offset>();
        attackArea = new LinkedList<Offset>();
        attackBorder = new LinkedList<Offset>();
        attackBorder2 = new LinkedList<Offset>();
        visitArea = new LinkedList<Offset>();
        visitBorder = new LinkedList<Offset>();
        attackOffsets = new HashMap<Offset, LinkedList<Offset>>();

        LinkedList<Offset> newOffsets = new LinkedList<Offset>();
        newOffsets.add(new Offset(0, 0,this));

        while (!newOffsets.isEmpty() && newOffsets.size() < 80) {

            Offset nextOffset = newOffsets.getFirst();
            view.add(nextOffset);
            newOffsets.remove(nextOffset);
            for (int i = 0; i < 4; i++) {
                Offset directionOffset = Offset.getDirectionOffset(i,this);
                Offset foundOffset = new Offset(directionOffset.getX()
                        + nextOffset.getX(), directionOffset.getY()
                        + nextOffset.getY(),this);
                if (!newOffsets.contains(foundOffset)
                        && !viewBorder.contains(foundOffset)
                        && !view.contains(foundOffset)) {
                    if (foundOffset.getTwoLengthSquared() <= viewRadius2) {
                        newOffsets.add(foundOffset);
                    } else {
                        viewBorder.add(foundOffset);
                    }
                }
            }
        }

        newOffsets.clear();
        newOffsets.add(new Offset(0, 0,this));
        attackArea.add(new Offset(0, 0,this));
        while (!newOffsets.isEmpty()) {

            Offset nextOffset;
            nextOffset = newOffsets.pop();

            for (int i = 0; i < 4; i++) {
                Offset directionOffset = Offset.getDirectionOffset(i,this);
                Offset foundOffset = new Offset(directionOffset.getX()
                        + nextOffset.getX(), directionOffset.getY()
                        + nextOffset.getY(),this);
                if (!attackArea.contains(foundOffset)
                        && foundOffset.getTwoLengthSquared() <= attackRadius2) {
                    newOffsets.add(foundOffset);
                    attackArea.add(foundOffset);
                } else if (foundOffset.getTwoLengthSquared() > attackRadius2) {
                    if (!attackBorder.contains(foundOffset))
                        attackBorder.add(foundOffset);
                    LinkedList<Offset> ao;
                    if (attackOffsets.containsKey(foundOffset)) {
                        ao = attackOffsets.get(foundOffset);
                    } else {
                        ao = new LinkedList<Offset>();
                        attackOffsets.put(foundOffset, ao);
                    }
                    ao.add(directionOffset);
                }
            }
        }

        for (Offset borderOffset : attackBorder) {
            for (int i = 0; i < 4; i++) {
                Offset directionOffset = Offset.getDirectionOffset(i,this);
                Offset foundOffset = new Offset(directionOffset.getX()
                        + borderOffset.getX(), directionOffset.getY()
                        + borderOffset.getY(),this);
                if (!attackArea.contains(foundOffset)
                        && !attackBorder.contains(foundOffset)) {
                    if (!attackBorder2.contains(foundOffset)) {
                        attackBorder2.add(foundOffset);
                    }
                    LinkedList<Offset> ao;
                    if (attackOffsets.containsKey(foundOffset)) {
                        ao = attackOffsets.get(foundOffset);
                    } else {
                        ao = new LinkedList<Offset>();
                        attackOffsets.put(foundOffset, ao);
                    }
                    ao.add(directionOffset);
                }
            }
        }

        newOffsets.clear();
        newOffsets.add(new Offset(0, 0,this));

        while (!newOffsets.isEmpty() && newOffsets.size() < 80) {

            Offset nextOffset = newOffsets.getFirst();
            visitArea.add(nextOffset);
            newOffsets.remove(nextOffset);

            for (int i = 0; i < 4; i++) {
                Offset directionOffset = Offset.getDirectionOffset(i,this);
                Offset foundOffset = new Offset(directionOffset.getX()
                        + nextOffset.getX(), directionOffset.getY()
                        + nextOffset.getY(),this);
                if (!newOffsets.contains(foundOffset)
                        && !visitBorder.contains(foundOffset)
                        && !visitArea.contains(foundOffset)) {
                    if (foundOffset.getTwoLengthSquared() <= visitRadius2) {
                        newOffsets.add(foundOffset);
                    } else {
                        visitBorder.add(foundOffset);
                    }
                }
            }
        }
    }



}
