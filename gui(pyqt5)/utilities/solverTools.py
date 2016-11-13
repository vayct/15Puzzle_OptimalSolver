import time
from PyQt5.QtCore import pyqtSignal, QThread
from PyQt5.QtGui import QPixmap

class SearchEngine(QThread):
    def __init__(self):
        super(SearchEngine, self).__init__()
        self._isRunning = False
        self._board = None
        self._solver = None

    def setProperties(self, board, solver):
        self._isRunning = False
        self._board = board
        self._solver = solver

    def run(self):
        if not self._isRunning:
            self._isRunning = True
        self._solver.findOptimalPath(self._board)

        while not self._solver.isSearchTimeout() and self._solver.moves() == -1:
            time.sleep(1)
            continue
        self._isRunning = False

class SearchStatus(QThread):
    statusTime = pyqtSignal(str)
    statusNodes = pyqtSignal(str)
    statusResult = pyqtSignal(str)
    statusMoves = pyqtSignal(str)
    widgetInaccessable = pyqtSignal(bool)
    widgetAccessable = pyqtSignal(bool)
    widgetAccessablePanda = pyqtSignal(bool)
    val0 = pyqtSignal(str)
    val1 = pyqtSignal(str)
    val2 = pyqtSignal(str)
    val3 = pyqtSignal(str)
    val4 = pyqtSignal(str)
    val5 = pyqtSignal(str)
    val6 = pyqtSignal(str)
    val7 = pyqtSignal(str)
    val8 = pyqtSignal(str)
    val9 = pyqtSignal(str)
    val10 = pyqtSignal(str)
    val11 = pyqtSignal(str)
    val12 = pyqtSignal(str)
    val13 = pyqtSignal(str)
    val14 = pyqtSignal(str)
    val15 = pyqtSignal(str)
    pic0 = pyqtSignal(QPixmap)
    pic1 = pyqtSignal(QPixmap)
    pic2 = pyqtSignal(QPixmap)
    pic3 = pyqtSignal(QPixmap)
    pic4 = pyqtSignal(QPixmap)
    pic5 = pyqtSignal(QPixmap)
    pic6 = pyqtSignal(QPixmap)
    pic7 = pyqtSignal(QPixmap)
    pic8 = pyqtSignal(QPixmap)
    pic9 = pyqtSignal(QPixmap)
    pic10 = pyqtSignal(QPixmap)
    pic11 = pyqtSignal(QPixmap)
    pic12 = pyqtSignal(QPixmap)
    pic13 = pyqtSignal(QPixmap)
    pic14 = pyqtSignal(QPixmap)
    pic15 = pyqtSignal(QPixmap)
    searchTimeout = pyqtSignal(bool)

    def __init__(self, image_panda):
        super(SearchStatus, self).__init__()
        self._isRunning = False
        self._search = None
        self._solver = None
        self._tiles = None
        self._zero = None
        self._use_image = None
        self._image_list = None
        self._image_panda = image_panda

    def setProperties(self, search, original, zero, use_image, image_list, speed):
        self._isRunning = False
        self._search = search
        self._solver = search._solver
        self._tiles = []
        for tile in original:
            self._tiles.append(tile)
        self._zero = zero
        self._use_image = use_image
        self._image_list = image_list
        self._speed = speed / 1000.0

    def run(self):
        self.widgetInaccessable.emit(True)
        self.widgetAccessable.emit(False)
        self.widgetAccessablePanda.emit(False)
        
        if not self._isRunning:
            self._isRunning = True

        while self._search._isRunning:
            self._time = "Time: " + str(self._solver.searchTime()) + "s"
            self._nodes = "Nodes: " + str(self._solver.searchNodeCount())        
            self._depth = "Depth: " + str(self._solver.searchDepth())
            self.statusTime.emit(self._time)
            self.statusNodes.emit(self._nodes)
            self.statusResult.emit(self._depth)
            time.sleep(0.1)

        self._time = "Time: " + str(self._solver.searchTime()) + "s"
        self._nodes = "Nodes: " + str(self._solver.searchNodeCount())        
        self.statusTime.emit(self._time)
        self.statusNodes.emit(self._nodes)
        if self._solver.isSearchTimeout():
            self.searchTimeout.emit(True)
            self.statusResult.emit("Result: timeout")
        else:
            steps = self._solver.moves()
            self.statusResult.emit("Result: " + str(steps) + " moves")
            movesString = self._solver.solutionQtString()
            self.statusMoves.emit(movesString)
            directions = self._solver.solution()
            strDir = []
            for idx in range (1, steps + 1):
                strDir.append(str(directions[idx]))

            time.sleep(1)

            for idx in range (0, steps):
                pos0 = self._zero
                pos = pos0
                strDirection = strDir[idx]
                if strDirection == 'RIGHT':
                    pos += 1
                elif strDirection == 'DOWN ':
                    pos +=4
                elif strDirection == 'LEFT ':
                    pos -= 1
                elif strDirection == 'UP   ':
                    pos -= 4
                else:
                    print("ERROR " + str(directions))
                self.statusMoves.emit(movesString + "\nStep " + str(idx + 1) + " : " + strDirection)
                val = self._tiles[pos]

                if self._use_image:
                    if pos == 0:
                        self.pic0.emit(QPixmap(""))
                    elif pos == 1:
                        self.pic1.emit(QPixmap(""))
                    elif pos == 2:
                        self.pic2.emit(QPixmap(""))
                    elif pos == 3:
                        self.pic3.emit(QPixmap(""))
                    elif pos == 4:
                        self.pic4.emit(QPixmap(""))
                    elif pos == 5:
                        self.pic5.emit(QPixmap(""))
                    elif pos == 6:
                        self.pic6.emit(QPixmap(""))
                    elif pos == 7:
                        self.pic7.emit(QPixmap(""))
                    elif pos == 8:
                        self.pic8.emit(QPixmap(""))
                    elif pos == 9:
                        self.pic9.emit(QPixmap(""))
                    elif pos == 10:
                        self.pic10.emit(QPixmap(""))
                    elif pos == 11:
                        self.pic11.emit(QPixmap(""))
                    elif pos == 12:
                        self.pic12.emit(QPixmap(""))
                    elif pos == 13:
                        self.pic13.emit(QPixmap(""))
                    elif pos == 14:
                        self.pic14.emit(QPixmap(""))
                    elif pos == 15:
                        self.pic15.emit(QPixmap(""))
                
                    if pos0 == 0:
                        self.pic0.emit(QPixmap(self._image_list[val]))
                    elif pos0 == 1:
                        self.pic1.emit(QPixmap(self._image_list[val]))
                    elif pos0 == 2:
                        self.pic2.emit(QPixmap(self._image_list[val]))
                    elif pos0 == 3:
                        self.pic3.emit(QPixmap(self._image_list[val]))
                    elif pos0 == 4:
                        self.pic4.emit(QPixmap(self._image_list[val]))
                    elif pos0 == 5:
                        self.pic5.emit(QPixmap(self._image_list[val]))
                    elif pos0 == 6:
                        self.pic6.emit(QPixmap(self._image_list[val]))
                    elif pos0 == 7:
                        self.pic7.emit(QPixmap(self._image_list[val]))
                    elif pos0 == 8:
                        self.pic8.emit(QPixmap(self._image_list[val]))
                    elif pos0 == 9:
                        self.pic9.emit(QPixmap(self._image_list[val]))
                    elif pos0 == 10:
                        self.pic10.emit(QPixmap(self._image_list[val]))
                    elif pos0 == 11:
                        self.pic11.emit(QPixmap(self._image_list[val]))
                    elif pos0 == 12:
                        self.pic12.emit(QPixmap(self._image_list[val]))
                    elif pos0 == 13:
                        self.pic13.emit(QPixmap(self._image_list[val]))
                    elif pos0 == 14:
                        self.pic14.emit(QPixmap(self._image_list[val]))
                    elif pos0 == 15:
                        self.pic15.emit(QPixmap(self._image_list[val]))
                else:
                    if pos == 0:
                        self.val0.emit("")
                    elif pos == 1:
                        self.val1.emit("")
                    elif pos == 2:
                        self.val2.emit("")
                    elif pos == 3:
                        self.val3.emit("")
                    elif pos == 4:
                        self.val4.emit("")
                    elif pos == 5:
                        self.val5.emit("")
                    elif pos == 6:
                        self.val6.emit("")
                    elif pos == 7:
                        self.val7.emit("")
                    elif pos == 8:
                        self.val8.emit("")
                    elif pos == 9:
                        self.val9.emit("")
                    elif pos == 10:
                        self.val10.emit("")
                    elif pos == 11:
                        self.val11.emit("")
                    elif pos == 12:
                        self.val12.emit("")
                    elif pos == 13:
                        self.val13.emit("")
                    elif pos == 14:
                        self.val14.emit("")
                    elif pos == 15:
                        self.val15.emit("")
                    
                    if pos0 == 0:
                        self.val0.emit(str(val))
                    elif pos0 == 1:
                        self.val1.emit(str(val))
                    elif pos0 == 2:
                        self.val2.emit(str(val))
                    elif pos0 == 3:
                        self.val3.emit(str(val))
                    elif pos0 == 4:
                        self.val4.emit(str(val))
                    elif pos0 == 5:
                        self.val5.emit(str(val))
                    elif pos0 == 6:
                        self.val6.emit(str(val))
                    elif pos0 == 7:
                        self.val7.emit(str(val))
                    elif pos0 == 8:
                        self.val8.emit(str(val))
                    elif pos0 == 9:
                        self.val9.emit(str(val))
                    elif pos0 == 10:
                        self.val10.emit(str(val))
                    elif pos0 == 11:
                        self.val11.emit(str(val))
                    elif pos0 == 12:
                        self.val12.emit(str(val))
                    elif pos0 == 13:
                        self.val13.emit(str(val))
                    elif pos0 == 14:
                        self.val14.emit(str(val))
                    elif pos0 == 15:
                        self.val15.emit(str(val))
                    
                self._tiles[pos0] = val
                self._tiles[pos] = 0
                self._zero = pos
                time.sleep(self._speed)

        if self._use_image:
            self.pic15.emit(QPixmap(self._image_list[0]))        
        
        time.sleep(0.5)
        self.widgetInaccessable.emit(False)
        self.widgetAccessable.emit(True)
        self.widgetAccessablePanda.emit(self._image_panda)
        self._isRunning = False    