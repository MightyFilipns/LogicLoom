# MCChipMaker
A Minecraft mod for creating logic circuit and computers

# Tutorial
First write the desire logic in a VHDL language supported by Yosys
Then use the Synth.ys script to create a JSON file this mod can load.

Then open a Minecraft world, preferably a superflat one, and configure the following

## Logic synthesis
Make sure you have installed Yosys.
Take the Synth.ys script from this repo and add read_* commands needed to load your design.

Run the script with
>yosys Synth.ys

The output JSON will be called `mc_chip_maker.json` load this file into MC using
>`/mcchipmaker load_json FILE_PATH

### Limitations 
Ports with Inout as direction are unsupported
The JSON file must only have one module. Only the first module is read all others are ignored.


## Setup parameters

Maximum Iteration for the placement step
>`/mcchipmaker param max_iter NUMBER_HERE

Size of the placement area along X and Y axis
>`/mcchipmaker param chip_size NUMBER_HERE

Force initial force multiplier used during spreading cells during placement
Should be a small value. Default is 0.05
>`/mcchipmaker param force_const NUMBER_HERE

Number by which to multiply the force multiplier every placement iteration
Should be a small value. Default is 1.047
>`/mcchipmaker param force_mul NUMBER_HERE

Path to the file made by Yosys
>`/mcchipmaker load_json FILE_PATH

### Fixed points

Fixed points are like other cells, but they have a fixed position. They are used to move the design as needed.
You will need to add at least one fixed point in order to get useful results.
Currently, you can only add fixed points that are attached to every cell in the design.

All ports in a design are a fixed point attached to the cells they are connected to with weights of 1.

Position of fixed point is stored relative to the current starting point.
If the starting point is changed after a fixed point is added the position of that fixed point in the world will change.

To modify fixed points use
>/chipmaker fixed_points


## P&R
Place the logic gates
Make sure that the area is completely flat and on all ground blocks redstone wire and repeaters can be placed
>`/mcchipmaker place
 
Place the Wires
>`/mcchipmaker route


## Fixing bad wires
Currently, the wire placing algorithm has bugs so run this command can be used to check for broken wires
This command compares starting point of all wires and checks if the power is the same at all ends.
If not problematic ones are printed in the chat. If there are any issues they are usually obvious.

This command can find bad wires even if everything appears to behave correctly as those bad wires might not be currently affecting the output.
You must wait for all signals to propagate otherwise you will bad results.
You can speed this up with /tick sprint
>`/mcchipmaker misc check_wires

To make sure all wire are checked you can use this command to force all wire to be

1. Powered
    >`/mcchipmaker misc force_power_wire ON 

2. Unpowered
    >`/mcchipmaker misc force_power_wire OFF

3. Returned to the normal state
    >`/mcchipmaker misc force_power_wire Normal

After running this command run the `check_wires` command
# Gallery
<html lang="EN_US">
<img src="/imgs/2026-02-02_10.56.12.png" width="400" alt="">
<div></div>
<img src="/imgs/2026-02-02_10.56.24.png" width="600" alt="">
</html>
