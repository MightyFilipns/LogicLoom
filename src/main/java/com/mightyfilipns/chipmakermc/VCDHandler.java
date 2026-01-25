package com.mightyfilipns.chipmakermc;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mightyfilipns.chipmakermc.Routing.HyperGraphNet;
import com.mightyfilipns.chipmakermc.Routing.Router;
import com.mightyfilipns.chipmakermc.Routing.TwoPinNet;
import net.minecraft.block.Blocks;
import net.minecraft.block.PistonBlock;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class VCDHandler
{
    static Map<Integer, String> id_netname = new HashMap<>();
    static Map<String, Integer> netname_toid = new HashMap<>();
    static Map<String, Boolean> g_valuemap = new HashMap<>();
    static Map<Integer, BlockPos> netid_toout_pos = null;

    public static void GetCurrentValuesAndCompare(ServerWorld w)
    {
        Map<String, Boolean> valuemap = new HashMap<>();
        netid_toout_pos = new HashMap<>();
        for (HyperGraphNet hyperGraphNet : Router.cached_hy)
        {
            var outpos = hyperGraphNet.all_points.get(hyperGraphNet.allpoints_pos).withY(Placer.last_pos.getY());
            var isext = w.getBlockState(outpos).get(RedstoneWireBlock.POWER) != 0;
            valuemap.put(id_netname.get(hyperGraphNet.net_id), isext);
            netid_toout_pos.put(hyperGraphNet.net_id, outpos);
        }
        for (TwoPinNet tpn : Router.cached_tpn)
        {
            var outpos = tpn.p1dir == JsonDesign.PortDirection.Output ? tpn.p1.withY(Placer.last_pos.getY()) : tpn.p2.withY(Placer.last_pos.getY());
            var isext = w.getBlockState(outpos).get(RedstoneWireBlock.POWER) != 0;
            valuemap.put(id_netname.get(tpn.id), isext);
            netid_toout_pos.put(tpn.id, outpos);
        }
        boolean found = false;
        for (Map.Entry<String, Boolean> en : valuemap.entrySet())
        {
            if (g_valuemap.get(en.getKey()) != en.getValue())
            {
                found = true;
                System.out.println("Mismatch in at: " + netid_toout_pos.get(netname_toid.get(en.getKey())) + " E: " + g_valuemap.get(en.getKey()) + " F: " + en.getValue());
            }
        }
        if (!found)
        {
            System.out.println("No mismatches found");
        }
    }

    public static void SetMap(JsonDesign.DesignModule m)
    {
        for (Map.Entry<String, JsonDesign.JsonWire> en : m.netnames.entrySet())
        {
            if (en.getValue().bits.size() == 1)
            {
                id_netname.put(en.getValue().bits.getFirst(), en.getKey());
                netname_toid.put(en.getKey(), en.getValue().bits.getFirst());
            }
            else
            {
                int i = 0;
                for (Integer bit : en.getValue().bits)
                {
                    id_netname.put(bit, en.getKey() + i);
                    netname_toid.put(en.getKey() + i, bit);
                    i++;
                }
            }
        }
    }

    public static void LoadVCD(String s)
    {
        Map<String, String> namemap = new HashMap<>();
        Map<String, Boolean> valuemap = new HashMap<>();
        List<String> d;
        try {
            d = Files.readAllLines(Path.of(s));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        int i1 = d.indexOf("  $scope module SimpleCalc $end");
        i1++;
        while(!Objects.equals(d.get(i1), "  $upscope $end"))
        {
            var sp = d.get(i1).trim().split(" ");
            int size = Integer.parseInt(sp[2]);
            String vcdname = sp[3];
            String realname = sp[4];
            namemap.put(vcdname, realname);
            i1++;
        }
        int datastart = d.indexOf("#0");
        for (int i = datastart; i < d.size(); i++)
        {
            String cl = d.get(i);
            if (cl.charAt(0) == 'b')
            {
                var vl = cl.split(" ");
                String vcdname = vl[1];
                String v = vl[0].substring(1);
                int i3 = v.length() - 1;
                for (int i2 : v.chars().toArray())
                {
                    valuemap.put(namemap.get(vcdname) + i3, i2 == '1');
                    i3--;
                }
            }
            else
            {
                boolean v = cl.charAt(0) == '1';
                String vcddump = cl.substring(1);
                String nm = namemap.get(vcddump);
                valuemap.put(nm, v);
            }
        }
        g_valuemap = valuemap;
    }
}
