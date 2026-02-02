# MCChipMaker
A Minecraft mod for creating logic circuit and computers

# Tutorial
First write the desire logic in a VHDL language supported by Yosys
Then use the Synth.ys script to create a JSON file this mod can load.

Then open a Minecraft world, preferably a superflat one, and configure the following

Maximum Iteration for the placement step
>`/mcchipmaker param max_iter NUMBER_HERE

Size of the placement area along X and Y axis
>`/mcchipmaker param chip_size NUMBER_HERE

Force multiplier used during spreading cells during placement
Should be a small value. Default is 0.05
>`/mcchipmaker param force_mul NUMBER_HERE

Path to the file made by Yosys
>`/mcchipmaker load_json FILE_PATH

Place the logic gates
Make sure that the area is completely flat and on all ground blocks redstone wire and repeaters can be placed
>`/mcchipmaker place START_POSITION
 
Place the Wires
>`/mcchipmaker route test_hyper START_POSITION

Currently, the wire placing algorithm has bugs so run this command to check for broken wires
This command compares start point of all wires and check if the power is the same at all ends.
If not problematic ones are print in the chat. If there are any issues they are usually obvious.

This command can find bad wires even if everything appears to behave correctly as those bad wires might not be currently affecting the output.
However, it might not always find all bad wires. So you should change a few input signals and rerun this command.
You must wait for all signals to propagate otherwise you will bad results.
You can speed this up with /tick sprint

>`/mcchipmaker debug check_wires

# Gallery
<html lang="EN_US">
<img src="/imgs/2026-02-02_10.56.12.png" width="400" alt="">
<div></div>
<img src="/imgs/2026-02-02_10.56.24.png" width="600" alt="">
</html>
