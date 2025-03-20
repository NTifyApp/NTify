from tkinter import *
import tkinter
import os
import tksvg
from tkscrolledframe import ScrolledFrame

gui = tkinter.Tk()
gui.title("Available svgs viewer")

width = 600
height = 400
images = []

scroll_frame = ScrolledFrame(gui, width=width, height=height)
scroll_frame.pack(side='top', expand=True, fill='both')
scroll_frame.bind_scroll_wheel(gui)

frame = scroll_frame.display_widget(Frame)

width_of_image = width / 5
x = 0
y = 0
for file in os.listdir("src/main/resources/icons"):
    if x == 5:
        y += 1
        x = 0
    filename = os.path.splitext(os.path.basename(file))[0]
    if filename.endswith("white"): continue
    iframe = tkinter.Frame(frame, width=width_of_image, height=width_of_image + (width_of_image / 100 * 20))
    iframe.grid(column=x, row=y)
    svg_image = tksvg.SvgImage(data=open("src/main/resources/icons/" + file, "r").read(), scaletoheight=width_of_image)
    images.append(svg_image)
    tkinter.Label(iframe, image=svg_image).grid(column=0, row=0)
    tkinter.Label(iframe, text=filename).grid(column=0, row=1)
    x += 1

gui.wm_minsize(width + 110, height)
try:
    gui.mainloop()
except KeyboardInterrupt:
    pass