package e_economy.util;

import aic2024.user.*;

public class Mover {
    private final MapRecorder mapRecorder;
    private final UnitController uc;
    private int currentTurnDir;
    private int lastPathingTurn;

    public Mover(UnitController uc, MapRecorder mapRecorder) {
        this.mapRecorder = mapRecorder;
        this.uc = uc;
        currentTurnDir = uc.getRandomDouble() < 0.5 ? 1 : 0;
        lastPathingTurn = uc.getRound();
    }

    // ========== Bug nav ==========
    // Copied from https://github.com/carlguo866/battlecode23-gonefishin/blob/main/src/bot1/Unit.java
    // See their writeup at https://www.overleaf.com/project/63dd936dc0408b2539a1ac7e
    private final int PRV_LENGTH = 60;
    private final Direction[] prv = new Direction[PRV_LENGTH];
    private int pathingCnt = 0;
    private Location lastPathingTarget = null;
    private Location lastLocation = null;
    private int stuckCnt = 0;

    private final Direction[] prv_ = new Direction[PRV_LENGTH];
    final int MAX_DEPTH = 15;

    public void moveToward(Location location) {
//        uc.println(location);
        // reset queue when target location changes or there's gap in between calls
        if (!location.equals(lastPathingTarget) || lastPathingTurn < uc.getRound() - 4) {
            pathingCnt = 0;
            stuckCnt = 0;
        }
//        uc.println("start path stuck " + pathingCnt + " " + stuckCnt);

        if (uc.getAstronautInfo().getCurrentMovementCooldown() < GameConstants.MOVEMENT_COOLDOWN) {
            if (uc.getLocation().equals(lastLocation)) {
                if (uc.getRound() != lastPathingTurn) {
                    stuckCnt++;
                }
            } else {
                lastLocation = uc.getLocation();
                stuckCnt = 0;
            }
            lastPathingTarget = location;
            lastPathingTurn = uc.getRound();
            if (stuckCnt >= 3) {
//                uc.println("random move");
                Util.makeRandomMove(uc);
                pathingCnt = 0;
            }
            if (pathingCnt == 0) {
                //if free of obstacle: try go directly to target
                Direction dir = uc.getLocation().directionTo(location);
//                uc.println("dir " + dir);
                boolean dirCanPass = canPass(dir);
                boolean dirRightCanPass = canPass(dir.rotateRight());
                boolean dirLeftCanPass = canPass(dir.rotateLeft());
                if (dirCanPass || dirRightCanPass || dirLeftCanPass) {
                    if (dirCanPass && Util.tryMove(uc, dir)) ;
                    else if (dirRightCanPass && Util.tryMove(uc, dir.rotateRight())) ;
                    else if (dirLeftCanPass && Util.tryMove(uc, dir.rotateLeft())) ;
                } else {
                    //encounters obstacle; run simulation to determine best way to go
                    currentTurnDir = getTurnDir(dir, location);
//                    uc.println("cur " + currentTurnDir);
                    while (!canPass(dir) && pathingCnt != 8) {
                        prv[pathingCnt] = dir;
                        pathingCnt++;
                        if (currentTurnDir == 0) dir = dir.rotateLeft();
                        else dir = dir.rotateRight();
                    }
//                    uc.println("trying " + dir);
                    if (pathingCnt != 8 && Util.tryMove(uc, dir)) ;
                }
            } else {
                //update stack of past directions, move to next available direction
                if (pathingCnt > 1 && canPass(prv[pathingCnt - 2])) {
//                    uc.println("unwind " + pathingCnt + " " + prv[pathingCnt - 2]);
                    pathingCnt -= 2;
                }
                while (pathingCnt > 0 && canPass(prv[pathingCnt - 1])) {
//                    uc.setIndicatorLine(uc.getLocation(), uc.getLocation().add(prv[pathingCnt - 1]), 0, 255, 0);
//                    uc.println("dec " + pathingCnt + " " + prv[pathingCnt - 1]);
                    pathingCnt--;
                }
//                uc.println("new path " + pathingCnt);
                if (pathingCnt == 0) {
                    Direction dir = uc.getLocation().directionTo(location);
                    if (!canPass(dir)) {
                        prv[pathingCnt++] = dir;
                    }
                }
                int pathingCntCutOff = Math.min(PRV_LENGTH, pathingCnt + 8); // if 8 then all dirs blocked
//                uc.println("cutoff " + pathingCntCutOff);
                while (pathingCnt > 0 && !canPass(currentTurnDir == 0 ? prv[pathingCnt - 1].rotateLeft() : prv[pathingCnt - 1].rotateRight())) {
                    prv[pathingCnt] = currentTurnDir == 0 ? prv[pathingCnt - 1].rotateLeft() : prv[pathingCnt - 1].rotateRight();
//                    uc.println("add " + prv[pathingCnt]);
                    pathingCnt++;
                    if (pathingCnt == pathingCntCutOff) {
                        pathingCnt = 0;
                        return;
                    }
                }
                Direction moveDir = pathingCnt == 0 ? prv[pathingCnt] :
                        (currentTurnDir == 0 ? prv[pathingCnt - 1].rotateLeft() : prv[pathingCnt - 1].rotateRight());
//                uc.println("moveDir " + moveDir);
                Util.tryMove(uc, moveDir);
            }
        }
        lastPathingTarget = location;
        lastPathingTurn = uc.getRound();
    }

    private int getSteps(Location a, Location b) {
        int xdif = a.x - b.x;
        int ydif = a.y - b.y;
        if (xdif < 0) xdif = -xdif;
        if (ydif < 0) ydif = -ydif;
        if (xdif > ydif) return xdif;
        else return ydif;
    }

    private final int BYTECODE_CUTOFF = 3000;

    // this simulates turning left and right to find the best direction
    private int getTurnDir(Direction direction, Location target) {
//        uc.println("getTurnDir " + direction + " " + target);
        Location now = uc.getLocation();
        int moveLeft = 0;
        int moveRight = 0;

        //simulate turning left
        int pathingCnt_ = 0;
        Direction dir = direction;
        while (!canPass(now.add(dir)) && pathingCnt_ != 8) {
            prv_[pathingCnt_] = dir;
            pathingCnt_++;
            dir = dir.rotateLeft();
        }
//        uc.println("left start " + dir);
        now = now.add(dir);

        int byteCodeRem = uc.getEnergyLeft();
        if (byteCodeRem < BYTECODE_CUTOFF)
            return uc.getRandomDouble() < 0.5 ? 1 : 0;
        while (pathingCnt_ > 0) {
            moveLeft++;
            if (moveLeft > MAX_DEPTH) {
                break;
            }
            if (uc.getEnergyLeft() < BYTECODE_CUTOFF) {
                moveLeft = -1;
                break;
            }
            while (pathingCnt_ > 0 && canPass(now.add(prv_[pathingCnt_ - 1]))) {
                pathingCnt_--;
            }
            if (pathingCnt_ > 1 && canPass(now.add(prv_[pathingCnt_ - 1]))) {
                pathingCnt_ -= 2;
            }
            while (pathingCnt_ > 0 && !canPass(now.add(prv_[pathingCnt_ - 1].rotateLeft()))) {
                prv_[pathingCnt_] = prv_[pathingCnt_ - 1].rotateLeft();
                pathingCnt_++;
                if (pathingCnt_ > 8) {
                    moveLeft = -1;
                    break;
                }
            }
            if (pathingCnt_ > 8 || pathingCnt_ == 0) break;
            now = now.add(prv_[pathingCnt_ - 1].rotateLeft());
        }
        Location leftend = now;
//        uc.println("leftend " + leftend);

        //simulate turning right
        pathingCnt_ = 0;
        now = uc.getLocation();
        dir = direction;
        while (!canPass(dir) && pathingCnt_ != 8) {
            prv_[pathingCnt_] = dir;
            pathingCnt_++;
            dir = dir.rotateRight();
        }
        now = now.add(dir);
//        uc.println("right start " + dir);

        while (pathingCnt_ > 0) {
            moveRight++;
            if (moveRight > MAX_DEPTH) {
                break;
            }
            if (uc.getEnergyLeft() < BYTECODE_CUTOFF) {
                moveRight = -1;
                break;
            }
            while (pathingCnt_ > 0 && canPass(now.add(prv_[pathingCnt_ - 1]))) {
                pathingCnt_--;
            }
            if (pathingCnt_ > 1 && canPass(now.add(prv_[pathingCnt_ - 1]))) {
                pathingCnt_ -= 2;
            }
            while (pathingCnt_ > 0 && !canPass(now.add(prv_[pathingCnt_ - 1].rotateRight()))) {
                prv_[pathingCnt_] = prv_[pathingCnt_ - 1].rotateRight();
                pathingCnt_++;
                if (pathingCnt_ > 8) {
                    moveRight = -1;
                    break;
                }
            }
            if (pathingCnt_ > 8 || pathingCnt_ == 0) break;
            now = now.add(prv_[pathingCnt_ - 1].rotateRight());
        }
        Location rightend = now;
//        uc.println("rightend " + rightend);

        if (moveLeft == -1 || moveRight == -1) return uc.getRandomDouble() < 0.5 ? 1 : 0;
        return moveLeft + getSteps(leftend, target) <= moveRight + getSteps(rightend, target) ? 0 : 1;
    }

    private boolean canPass(Location loc) {
        if (loc.equals(uc.getLocation())) return true;
        if (uc.isOutOfMap(loc) || !mapRecorder.maybePassable(loc)) return false;
        if (!uc.canSenseLocation(loc)) return true;
        if (uc.senseTileType(loc) == TileType.WATER) return false;
        return uc.senseAstronaut(loc) == null && uc.senseStructure(loc) == null;
    }

    private boolean canPass(Direction dir) {
        Location loc = uc.getLocation().add(dir);
        if (uc.isOutOfMap(loc) || !mapRecorder.maybePassable(loc)) return false;
        if (!uc.canSenseLocation(loc)) return true;
        if (uc.senseTileType(loc) == TileType.WATER) return false;
        if (uc.senseAstronaut(loc) == null && uc.senseStructure(loc) == null) return true;
        return uc.getRandomDouble() < 0.25;
    }
}
