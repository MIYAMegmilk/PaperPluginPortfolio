# WeaponModes

モード切替式の武器システム。config.ymlで武器と複数のモードを定義し、右クリックでモードを切り替える。各モードに表示名・ロア・属性モディファイア・ヒット時効果・装備中バフを設定可能。

## 動作環境

- Paper 1.21.1
- Java 21

## 導入方法

1. ビルド: `./gradlew build`
2. `build/libs/WeaponModes-1.0.0.jar` を `plugins/` にコピー
3. サーバー起動でサンプル武器入りの `config.yml` が自動生成される
4. `config.yml` を編集して武器とモードを定義

## コマンド

| コマンド | 権限 | 説明 |
|---------|------|------|
| `/wm give <武器名>` | `weaponmodes.admin`（OP） | 武器をプレイヤーに付与 |
| `/wm list` | `weaponmodes.admin`（OP） | 登録済み武器の一覧表示 |

## config.yml

```yaml
weapons:
  phantom_blade:
    display-name: "Phantom Blade"
    base-material: NETHERITE_SWORD       # Material列挙名
    modes:
      normal:
        display-name: "&fPhantom Blade &8[&7Normal&8]"
        lore:
          - "&7A balanced stance."
        attributes:
          attack_damage:                  # 任意のキー（NamespacedKeyとして使用）
            attribute: GENERIC_ATTACK_DAMAGE
            value: 8.0
            operation: ADD_NUMBER         # ADD_NUMBER | ADD_SCALAR | MULTIPLY_SCALAR_1
            slot: MAINHAND               # MAINHAND | OFFHAND | HEAD | CHEST | LEGS | FEET | ANY

      offensive:
        display-name: "&cPhantom Blade &8[&4Offensive&8]"
        attributes:
          attack_damage:
            attribute: GENERIC_ATTACK_DAMAGE
            value: 14.0
            operation: ADD_NUMBER
            slot: MAINHAND
        on-hit:
          lifesteal: 0.20                 # 与ダメージの20%を回復
          effects:                        # 被弾者に付与するポーション効果
            - "SLOWNESS:1:60"             # TYPE:LEVEL:DURATION_TICKS

      defensive:
        display-name: "&9Phantom Blade &8[&9Defensive&8]"
        attributes:
          generic_armor:
            attribute: GENERIC_ARMOR
            value: 10.0
            operation: ADD_NUMBER
            slot: MAINHAND
        while-held:
          effects:                        # 武器を持っている間の継続効果
            - "RESISTANCE:1"
```

### モード機能一覧

| 機能 | 設定キー | 説明 |
|-----|---------|------|
| 表示 | `display-name`, `lore` | モードごとに武器名とロアが変化 |
| 属性 | `attributes` | Minecraftの属性モディファイアを適用 |
| ライフスティール | `on-hit.lifesteal` | 与ダメージの一定割合を回復（0.0〜1.0） |
| ヒット時デバフ | `on-hit.effects` | 攻撃時に相手にポーション効果を付与 |
| 装備中バフ | `while-held.effects` | メインハンドに持っている間ポーション効果を継続付与 |

## 仕組み

- **モード保存**: PDC（`weaponmodes:mode`）にモードインデックスを保存。名前ではなくインデックスで管理するため、モード名変更に強い
- **モード切替**: `PlayerInteractEvent` で右クリックを検出。切替前に `WeaponModeSwitchEvent`（キャンセル可能）を発火
- **ヒット時処理**: `EntityDamageByEntityEvent` でPDCからモードを解決し、ライフスティールとデバフを適用
- **装備中効果**: `PlayerItemHeldEvent` でプレイヤーごとにタスクを起動。武器を外すとタスクをキャンセル（グローバルポーリングなし）

## イベント

| イベント | タイミング | キャンセル |
|---------|-----------|----------|
| `WeaponModeSwitchEvent` | プレイヤーが右クリックでモード切替時 | 可 |

```java
@EventHandler
public void onSwitch(WeaponModeSwitchEvent event) {
    // 体力が低いときは攻撃モードへの切替を阻止
    if (event.getToMode().key().equals("offensive")
            && event.getPlayer().getHealth() < 6) {
        event.setCancelled(true);
        event.getPlayer().sendMessage("体力が低すぎます！");
    }
}
```

## ライセンス

MIT
