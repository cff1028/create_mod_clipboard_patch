package com.example.createbugfix;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;

@Mod("clipboard_patch")
public class ClipboardFixMod {
    // 白名单组件
    private static final ResourceLocation CLIPBOARD_PAGES = ResourceLocation.parse("create:clipboard_pages");
    private static final ResourceLocation CLIPBOARD_TYPE = ResourceLocation.parse("create:clipboard_type");

    public ClipboardFixMod(IEventBus modEventBus) {
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onItemToss(ItemTossEvent event) {
        ItemEntity itemEntity = event.getEntity();
        ItemStack originalStack = itemEntity.getItem();
        
        // 检查是否为剪贴板
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(originalStack.getItem());
        if (itemId == null || !itemId.toString().equals("create:clipboard")) {
            return;
        }
        
        // 获取组件映射
        DataComponentMap components = originalStack.getComponents();
        boolean hasIllegalComponents = false;
        
        // 检查是否有非白名单组件
        for (DataComponentType<?> componentType : components.keySet()) {
            ResourceLocation componentId = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(componentType);
            if (componentId != null && 
                !componentId.equals(CLIPBOARD_PAGES) && 
                !componentId.equals(CLIPBOARD_TYPE)) {
                hasIllegalComponents = true;
                break;
            }
        }
        
        // 如果有非法组件则修复
        if (hasIllegalComponents) {
            ItemStack newStack = new ItemStack(originalStack.getItem(), originalStack.getCount());
            
            // 复制白名单组件
            DataComponentType<?> pagesType = BuiltInRegistries.DATA_COMPONENT_TYPE.get(CLIPBOARD_PAGES);
            DataComponentType<?> typeType = BuiltInRegistries.DATA_COMPONENT_TYPE.get(CLIPBOARD_TYPE);
            
            if (pagesType != null && components.has(pagesType)) {
                copyComponent(newStack, pagesType, components.get(pagesType));
            }
            
            if (typeType != null && components.has(typeType)) {
                copyComponent(newStack, typeType, components.get(typeType));
            }
            
            // 替换物品
            itemEntity.setItem(newStack);
            
            // 可选：记录修复日志
            System.out.println("Fixed clipboard with illegal components");
        }
    }
    
    // 类型安全的组件复制方法
    @SuppressWarnings("unchecked")
    private <T> void copyComponent(ItemStack stack, DataComponentType<?> componentType, Object value) {
        try {
            stack.set((DataComponentType<T>) componentType, (T) value);
        } catch (ClassCastException e) {
            // 类型转换失败时忽略该组件
            System.err.println("Failed to copy component: " + componentType + " - " + e.getMessage());
        }
    }
}