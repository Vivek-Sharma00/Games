import tkinter as tk
from tkinter import messagebox
from enum import Enum, auto
from typing import List, Optional
 
class State(Enum):
    IDLE = auto()
    LIFTING = auto()
    SLIDING = auto()
    DROPPING = auto()

class SeniorHanoi:
    """
    Optimized Tower of Hanoi with custom state-reset logic.
    Complexity:
        - Move Validation: O(1)
        - Game Reset: O(n) where n is the number of disks.
    """
    def __init__(self, root: tk.Tk, disk_count: int = 5):
        self.root = root
        self.root.title("Tower of Hanoi - Professional Edition")
        
        # Configuration Constants
        self.DISK_COUNT = disk_count
        self.PEG_X = [150, 300, 450]
        self.BASE_Y = 350
        self.DISK_HEIGHT = 25
        self.LIFT_Y = 80
        self.ANIM_SPEED = 20
        
        # Internal State
        self._init_logic_state()
        
        # UI Setup
        self.canvas = tk.Canvas(root, width=600, height=450, bg="#ffffff", highlightthickness=0)
        self.canvas.pack()
        
        self.status_label = tk.Label(root, text=f"Moves: {self.moves}", font=("Segoe UI", 12, "bold"))
        self.status_label.pack(pady=10)

        self._setup_ui()
        self._spawn_disks()
        
        # Event Binding
        self.canvas.bind("<Button-1>", self._on_click)

    def _init_logic_state(self):
        """Initializes or resets the logical game state."""
        self.pegs: List[List[int]] = [[], [], []]
        self.moves = 0
        self.selected_peg: Optional[int] = None
        self.is_animating = False
        self.anim_state = State.IDLE

    def _setup_ui(self):
        """Draws the environment and peg hitboxes."""
        self.canvas.create_rectangle(50, self.BASE_Y, 550, self.BASE_Y + 15, fill="#2c3e50", outline="")
        for i, x in enumerate(self.PEG_X):
            self.canvas.create_line(x, 150, x, self.BASE_Y, width=6, fill="#bdc3c7")
            self.canvas.create_rectangle(x-50, 120, x+50, self.BASE_Y, fill="", outline="", tags=f"peg_{i}")

    def _spawn_disks(self):
        """Initializes disks on the source peg."""
        colors = ["#FF595E", "#FFCA3A", "#8AC926", "#1982C4", "#6A4C93"]
        for i in range(self.DISK_COUNT, 0, -1):
            width = 40 + (i * 25)
            disk = self.canvas.create_rectangle(0, 0, 0, 0, fill=colors[i % 5], outline="#2c3e50")
            self.canvas.itemconfig(disk, tags=(f"size_{i}",)) 
            self.pegs[0].append(disk)
            self._snap_to_position(disk, 0, len(self.pegs[0]) - 1)

    def _snap_to_position(self, disk: int, peg_idx: int, stack_pos: int):
        size_tag = [t for t in self.canvas.gettags(disk) if t.startswith("size_")][0]
        i = int(size_tag.split("_")[1])
        width = 40 + (i * 25)
        x = self.PEG_X[peg_idx]
        y = self.BASE_Y - (stack_pos * self.DISK_HEIGHT) - (self.DISK_HEIGHT / 2)
        self.canvas.coords(disk, x - width/2, y - self.DISK_HEIGHT/2, x + width/2, y + self.DISK_HEIGHT/2)

    def _on_click(self, event: tk.Event):
        if self.is_animating: return
        distances = [abs(event.x - x) for x in self.PEG_X]
        clicked_peg = distances.index(min(distances))
        if distances[clicked_peg] > 60: return

        if self.selected_peg is None:
            if self.pegs[clicked_peg]:
                self.selected_peg = clicked_peg
                self.canvas.itemconfig(self.pegs[clicked_peg][-1], outline="#3498db", width=4)
        else:
            self._process_move(self.selected_peg, clicked_peg)
            self.selected_peg = None

    def _process_move(self, src: int, dest: int):
        disk = self.pegs[src][-1]
        self.canvas.itemconfig(disk, outline="#2c3e50", width=1)
        if src == dest: return

        def get_size(d):
            return int([t for t in self.canvas.gettags(d) if t.startswith("size_")][0].split("_")[1])

        if self.pegs[dest] and get_size(disk) > get_size(self.pegs[dest][-1]):
            messagebox.showwarning("Illegal Move", "Larger disks cannot sit on smaller ones.")
            return

        self.pegs[src].pop()
        target_x = self.PEG_X[dest]
        target_y = self.BASE_Y - (len(self.pegs[dest]) * self.DISK_HEIGHT) - (self.DISK_HEIGHT / 2)
        self.pegs[dest].append(disk)
        self.moves += 1
        self.status_label.config(text=f"Moves: {self.moves}")
        self.is_animating = True
        self.anim_state = State.LIFTING
        self._animate_loop(disk, target_x, target_y)

    def _animate_loop(self, disk: int, tx: float, ty: float):
        coords = self.canvas.coords(disk)
        cx, cy = (coords[0] + coords[2]) / 2, (coords[1] + coords[3]) / 2
        if self.anim_state == State.LIFTING:
            if cy > self.LIFT_Y: self.canvas.move(disk, 0, -min(self.ANIM_SPEED, cy - self.LIFT_Y))
            else: self.anim_state = State.SLIDING
        elif self.anim_state == State.SLIDING:
            dist = abs(cx - tx)
            if dist > 0.1: self.canvas.move(disk, (1 if tx > cx else -1) * min(self.ANIM_SPEED, dist), 0)
            else: self.anim_state = State.DROPPING
        elif self.anim_state == State.DROPPING:
            if cy < ty: self.canvas.move(disk, 0, min(self.ANIM_SPEED, ty - cy))
            else:
                self.is_animating = False
                self.anim_state = State.IDLE
                self._check_victory()
                return
        self.root.after(10, lambda: self._animate_loop(disk, tx, ty))

    def _check_victory(self):
        """Win condition check: Peg 2 must hold all disks."""
        if len(self.pegs[2]) == self.DISK_COUNT:
            self._show_victory_popup()

    def _show_victory_popup(self):
        """Custom Toplevel window for Restart/Quit logic."""
        popup = tk.Toplevel(self.root)
        popup.title("Victory!")
        popup.geometry("300x150")
        popup.resizable(False, False)
        # Ensure it stays on top and grabs focus
        popup.transient(self.root)
        popup.grab_set()

        label = tk.Label(popup, text=f"Solved in {self.moves} moves!\nWhat would you like to do?", pady=20)
        label.pack()

        btn_frame = tk.Frame(popup)
        btn_frame.pack(fill="x", padx=20)

        # OK Button -> Quit
        tk.Button(btn_frame, text="OK (Quit)", width=10, command=self.root.destroy).pack(side="left", expand=True)
        # Restart Button -> New Game
        tk.Button(btn_frame, text="Restart", width=10, command=lambda: self._restart_game(popup)).pack(side="right", expand=True)

    def _restart_game(self, popup_window: tk.Toplevel):
        """Resets the environment and logic for a new game."""
        popup_window.destroy()
        self.canvas.delete("all")
        self._init_logic_state()
        self.status_label.config(text=f"Moves: {self.moves}")
        self._setup_ui()
        self._spawn_disks()

if __name__ == "__main__":
    app_root = tk.Tk()
    SeniorHanoi(app_root)
    app_root.mainloop()